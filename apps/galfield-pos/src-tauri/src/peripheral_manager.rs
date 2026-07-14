/// Event-driven peripheral listener manager.
///
/// Architecture: two threads per peripheral.
///   reader thread  — blocks on the port file; forwards raw lines to an mpsc channel.
///   processor thread — receives from that channel with a short timeout so it can
///                      check the stop flag without waiting for the next barcode.
///
/// This covers *continuous* listeners (barcode scanner, future fingerprint
/// reader). One-shot peripheral actions (printer, cash drawer) follow the
/// same event-naming convention and thread-per-job shape but don't need a
/// stop flag — see `invoices::trigger_print_invoice` /
/// `invoices::trigger_open_cash_drawer` and "Peripheral event model" in
/// CLAUDE.md.
///
/// Extending to new peripherals:
///   1. Add a new `spawn_<device>_listener` function below.
///   2. Handle the new `device_type` string in `start_peripheral_listener`.
///   3. Define new event names following the `peripheral-<device>-*` convention.
///   4. On the Vue side, add a new `on<Device>*` helper to `usePeripheralBus.ts`.

use rusqlite::{Connection, OpenFlags};
use serde::Serialize;
use std::io::BufRead;
use std::path::PathBuf;
use std::sync::{
    atomic::{AtomicBool, Ordering},
    mpsc, Arc,
};
use std::time::Duration;
use tauri::{AppHandle, Emitter, State};

use crate::logging;
use crate::AppState;

/// How often the reader thread's port poll wakes up to re-check the stop
/// flag, matching the processor thread's `recv_timeout` cadence below.
const POLL_TIMEOUT_MS: i32 = 150;

// ── Event payload types ───────────────────────────────────────────────────────

/// Matches the TypeScript `Product` interface so the frontend can add it to the
/// cart directly without an extra lookup round-trip.
#[derive(Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct ScannedProduct {
    pub id:             String,
    pub barcode:        String,
    pub product_name:   String,
    pub unit_price:     f64,
    pub category:       String,
    pub is_active:      bool,
    pub image_path:     String,
    pub image_hash:     String,
    pub stock_quantity: f64,
    pub last_sync_at:   Option<String>,
    pub created_at:     String,
    pub updated_at:     String,
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
struct BarcodeNotFoundPayload {
    barcode: String,
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct DeviceStatusPayload {
    pub connected: bool,
    pub port: String,
}

// ── DB helpers ────────────────────────────────────────────────────────────────

fn find_product_by_barcode(
    conn: &Connection,
    barcode: &str,
) -> rusqlite::Result<Option<ScannedProduct>> {
    let mut stmt = conn.prepare_cached(
        "SELECT CAST(id AS TEXT), barcode, product_name, unit_price,
                COALESCE(category, ''), is_active,
                image_path, image_hash, stock_quantity,
                last_sync_at, created_at, updated_at
         FROM   products
         WHERE  barcode = ?1 AND is_active = 1
         LIMIT  1",
    )?;

    let mut rows = stmt.query_map([barcode], |row| {
        Ok(ScannedProduct {
            id:             row.get(0)?,
            barcode:        row.get::<_, Option<String>>(1)?.unwrap_or_default(),
            product_name:   row.get(2)?,
            unit_price:     row.get(3)?,
            category:       row.get(4)?,
            is_active:      row.get::<_, i32>(5)? != 0,
            image_path:     row.get(6)?,
            image_hash:     row.get(7)?,
            stock_quantity: row.get(8)?,
            last_sync_at:   row.get(9)?,
            created_at:     row.get(10)?,
            updated_at:     row.get(11)?,
        })
    })?;

    Ok(rows.next().transpose()?)
}

// ── Barcode listener ──────────────────────────────────────────────────────────

fn spawn_barcode_listener(
    port: String,
    db_path: PathBuf,
    stop: Arc<AtomicBool>,
    app: AppHandle,
) {
    let (tx, rx) = mpsc::channel::<String>();
    let port_reader = port.clone();
    let app_reader  = app.clone();
    let stop_reader = Arc::clone(&stop);

    // ── Thread 1: reader ──────────────────────────────────────────────────────
    // Waits on the port file (via `poll`, see below) and sends each trimmed
    // line to the channel. Exits when the channel closes (processor thread
    // dropped rx), on IO error, or when `stop` is flipped.
    std::thread::Builder::new()
        .name(format!("peripheral-barcode-reader-{}", port_reader))
        .spawn(move || {
            let file = match std::fs::File::open(&port_reader) {
                Ok(f) => f,
                Err(e) => {
                    logging::step(
                        "peripheral_manager::spawn_barcode_listener",
                        format!("cannot open port {}: {}", port_reader, e),
                    );
                    app_reader
                        .emit(
                            "peripheral-barcode-error",
                            format!("Cannot open port {}: {}", port_reader, e),
                        )
                        .ok();
                    return;
                }
            };

            logging::step(
                "peripheral_manager::spawn_barcode_listener",
                format!("reader started on {}", port_reader),
            );

            let fd = std::os::unix::io::AsRawFd::as_raw_fd(&file);
            let mut reader = std::io::BufReader::new(file);
            let mut buf: Vec<u8> = Vec::new();

            loop {
                if stop_reader.load(Ordering::SeqCst) {
                    // Without this check, a reader stopped mid-wait (e.g. the
                    // user navigated away from POS) keeps blocking on the port
                    // until the *next* physical scan arrives — so switching
                    // back to POS starts a second reader on the same port
                    // while this stale one is still alive. Whichever of the
                    // two the OS hands the next scan's bytes to "wins"; if
                    // it's this one, it reads the barcode fine but fails to
                    // forward it (its processor already shut down) and only
                    // then exits — which is exactly the "first scan after
                    // switching views does nothing, second one works" pattern.
                    // Polling the port with a short timeout below lets a
                    // stopped reader notice and exit within POLL_TIMEOUT_MS,
                    // instead of waiting for a phantom scan to kill it.
                    logging::step(
                        "peripheral_manager::spawn_barcode_listener",
                        format!("reader for {} stopped", port_reader),
                    );
                    break;
                }

                let mut pfd = libc::pollfd { fd, events: libc::POLLIN, revents: 0 };
                let ready = unsafe { libc::poll(&mut pfd, 1, POLL_TIMEOUT_MS) };
                if ready == 0 {
                    continue; // timeout, no data yet — loop back and re-check stop
                }
                if ready < 0 {
                    logging::step(
                        "peripheral_manager::spawn_barcode_listener",
                        format!("poll() failed on {}", port_reader),
                    );
                    break;
                }

                buf.clear();
                match reader.read_until(b'\n', &mut buf) {
                    Ok(0) => {
                        // EOF: the port was closed (device unplugged).
                        logging::step(
                            "peripheral_manager::spawn_barcode_listener",
                            format!("port {} closed (EOF)", port_reader),
                        );
                        break;
                    }
                    Ok(_) => {
                        // Read raw bytes instead of requiring valid UTF-8. The old
                        // `BufRead::lines()` version errored out with `InvalidData` —
                        // and silently discarded — the *entire* line whenever a single
                        // stray non-UTF-8 byte showed up (scanner startup noise, a
                        // checksum byte, a cable glitch), which is what made scans
                        // intermittently vanish with no error and no trace. Filtering
                        // to printable ASCII keeps the real barcode digits/letters
                        // even when junk bytes are mixed in around them.
                        let barcode: String = buf
                            .iter()
                            .filter(|b| b.is_ascii_graphic())
                            .map(|&b| b as char)
                            .collect();

                        if barcode.is_empty() {
                            continue;
                        }

                        if tx.send(barcode).is_err() {
                            break; // processor dropped receiver → stop
                        }
                    }
                    Err(e) => {
                        logging::step(
                            "peripheral_manager::spawn_barcode_listener",
                            format!("reader IO error on {}: {}", port_reader, e),
                        );
                        break;
                    }
                }
            }
        })
        .ok();

    // ── Thread 2: processor ───────────────────────────────────────────────────
    // Receives barcodes from the reader with a short timeout so the stop flag
    // is checked frequently. Opens its own read-only DB connection.
    let app_proc = app.clone();
    std::thread::Builder::new()
        .name(format!("peripheral-barcode-processor-{}", port))
        .spawn(move || {
            let conn = match Connection::open_with_flags(
                &db_path,
                OpenFlags::SQLITE_OPEN_READ_ONLY | OpenFlags::SQLITE_OPEN_NO_MUTEX,
            ) {
                Ok(c) => c,
                Err(e) => {
                    app_proc
                        .emit("peripheral-barcode-error", format!("DB error: {}", e))
                        .ok();
                    return;
                }
            };

            app_proc
                .emit(
                    "peripheral-barcode-status",
                    DeviceStatusPayload { connected: true, port: port.clone() },
                )
                .ok();

            loop {
                if stop.load(Ordering::SeqCst) {
                    break;
                }

                match rx.recv_timeout(Duration::from_millis(150)) {
                    Ok(barcode) => {
                        match find_product_by_barcode(&conn, &barcode) {
                            Ok(Some(product)) => {
                                logging::step(
                                    "peripheral_manager::spawn_barcode_listener",
                                    format!("barcode '{}' matched product '{}'", barcode, product.product_name),
                                );
                                app_proc.emit("peripheral-barcode-found", product).ok();
                            }
                            Ok(None) => {
                                logging::step(
                                    "peripheral_manager::spawn_barcode_listener",
                                    format!("barcode '{}' matched no active product", barcode),
                                );
                                app_proc
                                    .emit(
                                        "peripheral-barcode-not-found",
                                        BarcodeNotFoundPayload { barcode },
                                    )
                                    .ok();
                            }
                            Err(e) => {
                                logging::step(
                                    "peripheral_manager::spawn_barcode_listener",
                                    format!("lookup for barcode '{}' failed: {}", barcode, e),
                                );
                                app_proc
                                    .emit(
                                        "peripheral-barcode-error",
                                        format!("DB query failed: {}", e),
                                    )
                                    .ok();
                            }
                        }
                    }
                    Err(mpsc::RecvTimeoutError::Timeout) => {
                        // Normal: no barcode yet; loop and re-check stop flag.
                    }
                    Err(mpsc::RecvTimeoutError::Disconnected) => {
                        // Reader thread died (port disconnected).
                        logging::step(
                            "peripheral_manager::spawn_barcode_listener",
                            format!("reader for {} disconnected, stopping processor", port),
                        );
                        app_proc
                            .emit(
                                "peripheral-barcode-status",
                                DeviceStatusPayload { connected: false, port: port.clone() },
                            )
                            .ok();
                        break;
                    }
                }
            }
        })
        .ok();
}

// ── Tauri commands ────────────────────────────────────────────────────────────

#[tauri::command]
pub fn start_peripheral_listener(
    device_type: String,
    port: String,
    state: State<'_, AppState>,
    app_handle: AppHandle,
) -> Result<(), String> {
    // Stop the existing listener for this device type, if any.
    {
        let stops = state.peripheral_stops.lock().map_err(|e| e.to_string())?;
        if let Some(existing) = stops.get(&device_type) {
            existing.store(true, Ordering::SeqCst);
        }
    }

    let stop = Arc::new(AtomicBool::new(false));

    {
        let mut stops = state.peripheral_stops.lock().map_err(|e| e.to_string())?;
        stops.insert(device_type.clone(), Arc::clone(&stop));
    }

    let db_path = {
        let db = state.db.lock().map_err(|e| e.to_string())?;
        db.path.clone()
    };

    match device_type.as_str() {
        "barcode" => spawn_barcode_listener(port, db_path, stop, app_handle),
        other => return Err(format!("Unknown peripheral device type: '{}'", other)),
    }

    Ok(())
}

#[tauri::command]
pub fn stop_peripheral_listener(
    device_type: String,
    state: State<'_, AppState>,
) -> Result<(), String> {
    let mut stops = state.peripheral_stops.lock().map_err(|e| e.to_string())?;

    if let Some(stop) = stops.remove(&device_type) {
        stop.store(true, Ordering::SeqCst);
    }

    Ok(())
}
