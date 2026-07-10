use serde::{Deserialize, Serialize};
use std::io::Write;
use std::sync::{Arc, Mutex};
use tauri::{AppHandle, Emitter, State};

use crate::peripheral_manager::DeviceStatusPayload;
use crate::AppState;

// ── create_sale ─────────────────────────────────────────────────────────────

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SaleItemInput {
    pub product_id: String,
    pub quantity: i64,
    pub unit_price: f64,
    pub subtotal: f64,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CreateSaleResult {
    pub sale_id: i64,
    pub invoice_number: String,
    pub created_at: String,
    pub change_due: f64,
}

#[tauri::command]
pub fn create_sale(
    state: State<AppState>,
    payment_method_id: i64,
    notes: String,
    items: Vec<SaleItemInput>,
    subtotal: f64,
    discount: f64,
    total: f64,
    amount_received: f64,
    invoice_prefix: String,
) -> Result<CreateSaleResult, String> {
    // A cashier who doesn't use the cash calculator sends 0 — treat that as
    // "exact payment, no change" instead of recording a bogus negative
    // change_due for sales that never used it.
    let amount_received = if amount_received > 0.0 { amount_received } else { total };
    let change_due = amount_received - total;

    let mut db = state.db.lock().map_err(|e| e.to_string())?;
    let tx = db.conn.transaction().map_err(|e| e.to_string())?;

    // `defaults.validate_stock` (Configuración → Por Defecto) gates whether
    // selling more than what's on hand is even allowed. Checked up front, in
    // the same transaction as the writes below, so a rejection here rolls
    // back cleanly (the transaction is simply never committed).
    let validate_stock: bool = tx
        .query_row(
            "SELECT value_property FROM app_settings WHERE key_property = 'defaults.validate_stock'",
            [],
            |row| row.get::<_, String>(0),
        )
        .map(|v| v == "true")
        .unwrap_or(false);

    if validate_stock {
        for item in &items {
            let (product_name, current_stock): (String, f64) = tx
                .query_row(
                    "SELECT product_name, stock_quantity FROM products WHERE id = ?1",
                    rusqlite::params![item.product_id],
                    |row| Ok((row.get(0)?, row.get(1)?)),
                )
                .map_err(|e| format!("Producto {} no encontrado: {}", item.product_id, e))?;

            if (item.quantity as f64) > current_stock {
                return Err(format!(
                    "Stock insuficiente para \"{}\": disponible {}, solicitado {}",
                    product_name, current_stock, item.quantity
                ));
            }
        }
    }

    // Generated up front so it exists from the very first insert - this is
    // the idempotency key `sales_sync::push_pending_sales` sends as the
    // cloud endpoint's `clientEventId` (see backend/pos's
    // POST /api/inventory/adjustments).
    let sync_uuid = uuid::Uuid::new_v4().to_string();

    tx.execute(
        "INSERT INTO sales (subtotal, discount, total, payment_method, notes, amount_received, change_due, sync_uuid)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
        rusqlite::params![subtotal, discount, total, payment_method_id, notes, amount_received, change_due, sync_uuid],
    )
    .map_err(|e| e.to_string())?;

    let sale_id = tx.last_insert_rowid();

    for item in &items {
        tx.execute(
            "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal)
             VALUES (?1, ?2, ?3, ?4, ?5)",
            rusqlite::params![sale_id, item.product_id, item.quantity, item.unit_price, item.subtotal],
        )
        .map_err(|e| e.to_string())?;

        // Local stock reflects sales immediately — this is what the cloud
        // push (future work, see CLAUDE.md's sync strategy) will eventually
        // report upstream; for now it's what keeps Inventory/POS stock
        // counts honest between catalog syncs.
        tx.execute(
            "UPDATE products SET stock_quantity = stock_quantity - ?1 WHERE id = ?2",
            rusqlite::params![item.quantity as f64, item.product_id],
        )
        .map_err(|e| e.to_string())?;
    }

    let created_at: String = tx
        .query_row(
            "SELECT created_at FROM sales WHERE id = ?1",
            rusqlite::params![sale_id],
            |row| row.get(0),
        )
        .map_err(|e| e.to_string())?;

    tx.commit().map_err(|e| e.to_string())?;

    Ok(CreateSaleResult {
        sale_id,
        invoice_number: format!("{}{:06}", invoice_prefix, sale_id),
        created_at,
        change_due,
    })
}

// ── print_invoice ────────────────────────────────────────────────────────────

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InvoicePrintPayload {
    pub store_name: String,
    pub store_tax_id: String,
    pub store_address: String,
    pub store_phone: String,
    /// `"58mm"` or `"80mm"` — matches `ConfigSettings.peripherals.printerPaperWidth`.
    pub paper_width: String,
    pub invoice_number: String,
    pub date: String,
    pub seller: String,
    pub customer: String,
    pub items: Vec<InvoicePrintItem>,
    pub subtotal: f64,
    pub discount: f64,
    pub total: f64,
    pub payment_method: String,
    /// 0 means the cashier didn't use the cash calculator for this sale —
    /// the "Efectivo recibido" / "Cambio" lines are omitted when this is 0.
    pub amount_received: f64,
    pub change_due: f64,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InvoicePrintItem {
    pub name: String,
    pub quantity: i64,
    pub line_total: f64,
}

const ESC: u8 = 0x1b;
const GS: u8 = 0x1d;

/// Thermal printers vary wildly in default code page support, and most don't
/// get an explicit `ESC t n` code page selection from us — so any non-ASCII
/// byte (Spanish accents, or the non-breaking space `Intl.NumberFormat`
/// likes to put between a currency symbol and its amount) prints as garbage
/// (e.g. "Bogotá" → "Bogotí", "$ 3.500" → "$Tá3.500"). The archived PDF still
/// keeps full accents; the printed ticket trades them for guaranteed-readable
/// plain ASCII on any ESC/POS printer.
fn to_printer_ascii(s: &str) -> String {
    s.chars()
        .map(|c| match c {
            'á' | 'à' | 'ä' | 'â' => 'a',
            'é' | 'è' | 'ë' | 'ê' => 'e',
            'í' | 'ì' | 'ï' | 'î' => 'i',
            'ó' | 'ò' | 'ö' | 'ô' => 'o',
            'ú' | 'ù' | 'ü' | 'û' => 'u',
            'Á' | 'À' | 'Ä' | 'Â' => 'A',
            'É' | 'È' | 'Ë' | 'Ê' => 'E',
            'Í' | 'Ì' | 'Ï' | 'Î' => 'I',
            'Ó' | 'Ò' | 'Ö' | 'Ô' => 'O',
            'Ú' | 'Ù' | 'Ü' | 'Û' => 'U',
            'ñ' => 'n',
            'Ñ' => 'N',
            'ç' => 'c',
            'Ç' => 'C',
            '¿' => '?',
            '¡' => '!',
            '\u{00A0}' => ' ', // non-breaking space
            c if c.is_ascii() => c,
            _ => '?',
        })
        .collect()
}

/// Formats an amount as plain ASCII with thousands separators (e.g. `3500.0`
/// → `"$ 3.500"`), matching the COP style used elsewhere in the app — done
/// in Rust instead of reusing the frontend's `Intl`-based formatter, since
/// that one inserts a non-breaking space that garbles on thermal printers.
fn format_money(amount: f64) -> String {
    let rounded = amount.round() as i64;
    let digits = rounded.unsigned_abs().to_string();

    let mut grouped = String::new();
    for (i, ch) in digits.chars().rev().enumerate() {
        if i > 0 && i % 3 == 0 {
            grouped.push('.');
        }
        grouped.push(ch);
    }
    let grouped: String = grouped.chars().rev().collect();

    if rounded < 0 {
        format!("-$ {}", grouped)
    } else {
        format!("$ {}", grouped)
    }
}

/// Character columns per line for each supported roll width, at the printer's
/// normal (non-condensed) font. 58mm printers fit ~32 columns, 80mm ~48.
fn line_width_for(paper_width: &str) -> usize {
    match paper_width {
        "58mm" => 32,
        _ => 48, // "80mm" and any unrecognized value
    }
}

const QTY_COL_WIDTH: usize = 5;
const PRICE_COL_WIDTH: usize = 11;

fn pad_right(s: &str, width: usize) -> String {
    let len = s.chars().count();
    if len > width {
        let mut truncated: String = s.chars().take(width.saturating_sub(1)).collect();
        truncated.push('.');
        truncated
    } else {
        format!("{}{}", s, " ".repeat(width - len))
    }
}

fn pad_left(s: &str, width: usize) -> String {
    let len = s.chars().count();
    if len >= width {
        s.to_string()
    } else {
        format!("{}{}", " ".repeat(width - len), s)
    }
}

fn center(s: &str, width: usize) -> String {
    let len = s.chars().count();
    if len >= width {
        return s.to_string();
    }
    let left = (width - len) / 2;
    let right = width - len - left;
    format!("{}{}{}", " ".repeat(left), s, " ".repeat(right))
}

fn separator(ch: char, width: usize) -> String {
    ch.to_string().repeat(width)
}

/// One right-aligned "Label: value" line, e.g. `"     Subtotal: $ 68.400"`.
fn total_line(label: &str, value: &str, width: usize) -> String {
    pad_left(&format!("{}: {}", label, value), width)
}

/// Appends the standard Epson-compatible `GS ( k` 2D symbol command sequence
/// to print a QR code: select model 2, set module size + error correction,
/// store the payload, then print it. Widely supported across ESC/POS clones.
fn append_qr_code(out: &mut Vec<u8>, data: &str) {
    let data = data.as_bytes();
    if data.is_empty() {
        return;
    }

    // Select model 2 (n1 = 0x32).
    out.extend_from_slice(&[GS, b'(', b'k', 0x04, 0x00, 0x31, 0x41, 0x32, 0x00]);
    // Module size, 1-16 dots per module — 6 balances scan distance and size.
    out.extend_from_slice(&[GS, b'(', b'k', 0x03, 0x00, 0x31, 0x43, 0x06]);
    // Error correction level: 48=L 49=M 50=Q 51=H.
    out.extend_from_slice(&[GS, b'(', b'k', 0x03, 0x00, 0x31, 0x45, 0x31]);

    // Store the data (length = payload + 3 bytes for cn/fn/m, little-endian).
    let store_len = data.len() + 3;
    out.extend_from_slice(&[
        GS,
        b'(',
        b'k',
        (store_len & 0xFF) as u8,
        ((store_len >> 8) & 0xFF) as u8,
        0x31,
        0x50,
        0x30,
    ]);
    out.extend_from_slice(data);

    // Print the stored symbol.
    out.extend_from_slice(&[GS, b'(', b'k', 0x03, 0x00, 0x31, 0x51, 0x30]);
}

/// Builds an ESC/POS byte stream styled like a real point-of-sale ticket:
/// centered store header with tax id, a column-aligned items table, a
/// right-aligned totals block, and a CODE39 barcode of the invoice number.
/// Column widths scale with `payload.paper_width` (58mm vs 80mm rolls).
fn build_escpos(payload: &InvoicePrintPayload) -> Vec<u8> {
    let mut out = Vec::new();
    let t = to_printer_ascii;
    let line = |s: String| format!("{}\n", s);

    let width = line_width_for(&payload.paper_width);
    let desc_col_width = width - QTY_COL_WIDTH - PRICE_COL_WIDTH;

    out.extend_from_slice(&[ESC, b'@']); // initialize printer

    // ── Store header — centered ────────────────────────────────────────────
    out.extend_from_slice(&[ESC, b'a', 1]); // center align
    out.extend_from_slice(&[ESC, b'E', 1]); // bold on
    out.extend_from_slice(line(t(&payload.store_name)).as_bytes());
    out.extend_from_slice(&[ESC, b'E', 0]); // bold off
    if !payload.store_tax_id.is_empty() {
        out.extend_from_slice(line(format!("NIT: {}", t(&payload.store_tax_id))).as_bytes());
    }
    out.extend_from_slice(line(t(&payload.store_address)).as_bytes());
    out.extend_from_slice(line(t(&payload.store_phone)).as_bytes());
    out.extend_from_slice(line(separator('=', width)).as_bytes());
    out.extend_from_slice(&[ESC, b'E', 1]);
    out.extend_from_slice(line(center("FACTURA DE VENTA", width)).as_bytes());
    out.extend_from_slice(&[ESC, b'E', 0]);
    out.extend_from_slice(line(separator('=', width)).as_bytes());

    // ── Sale meta — left-aligned ───────────────────────────────────────────
    out.extend_from_slice(&[ESC, b'a', 0]); // left align
    out.extend_from_slice(line(format!("Factura:  {}", t(&payload.invoice_number))).as_bytes());
    out.extend_from_slice(line(format!("Fecha:    {}", t(&payload.date))).as_bytes());
    out.extend_from_slice(line(format!("Vendedor: {}", t(&payload.seller))).as_bytes());
    out.extend_from_slice(line(format!("Cliente:  {}", t(&payload.customer))).as_bytes());
    out.extend_from_slice(line(separator('-', width)).as_bytes());

    // ── Items table ─────────────────────────────────────────────────────────
    out.extend_from_slice(&[ESC, b'E', 1]);
    out.extend_from_slice(
        line(format!(
            "{}{}{}",
            pad_right("Cant", QTY_COL_WIDTH),
            pad_right("Descripcion", desc_col_width),
            pad_left("Valor", PRICE_COL_WIDTH),
        ))
        .as_bytes(),
    );
    out.extend_from_slice(&[ESC, b'E', 0]);
    out.extend_from_slice(line(separator('-', width)).as_bytes());

    for item in &payload.items {
        out.extend_from_slice(
            line(format!(
                "{}{}{}",
                pad_right(&item.quantity.to_string(), QTY_COL_WIDTH),
                pad_right(&t(&item.name), desc_col_width),
                pad_left(&format_money(item.line_total), PRICE_COL_WIDTH),
            ))
            .as_bytes(),
        );
    }

    out.extend_from_slice(line(separator('-', width)).as_bytes());
    out.extend_from_slice(line(format!("No. Articulos: {}", payload.items.len())).as_bytes());
    out.extend_from_slice(line(separator('-', width)).as_bytes());

    // ── Totals — right-aligned ─────────────────────────────────────────────
    out.extend_from_slice(line(total_line("Subtotal", &format_money(payload.subtotal), width)).as_bytes());
    out.extend_from_slice(line(total_line("Descuento", &format_money(payload.discount), width)).as_bytes());
    out.extend_from_slice(&[ESC, b'E', 1]);
    out.extend_from_slice(line(total_line("TOTAL", &format_money(payload.total), width)).as_bytes());
    out.extend_from_slice(&[ESC, b'E', 0]);
    out.extend_from_slice(line(total_line("Pago", &t(&payload.payment_method), width)).as_bytes());

    if payload.amount_received > 0.0 {
        out.extend_from_slice(
            line(total_line("Efectivo", &format_money(payload.amount_received), width)).as_bytes(),
        );
        out.extend_from_slice(&[ESC, b'E', 1]);
        out.extend_from_slice(
            line(total_line("Cambio", &format_money(payload.change_due), width)).as_bytes(),
        );
        out.extend_from_slice(&[ESC, b'E', 0]);
    }

    out.extend_from_slice(line(separator('=', width)).as_bytes());

    // ── Footer — thank-you message + QR code with the invoice summary ─────
    out.extend_from_slice(&[ESC, b'a', 1]); // center align
    out.extend_from_slice(line(String::new()).as_bytes());
    out.extend_from_slice(line("Gracias por su compra!".to_string()).as_bytes());
    out.extend_from_slice(line(String::new()).as_bytes());

    let qr_data = format!(
        "Factura: {}\nFecha: {}\nTotal: {}",
        t(&payload.invoice_number),
        t(&payload.date),
        format_money(payload.total),
    );
    append_qr_code(&mut out, &qr_data);

    out.extend_from_slice(b"\n\n");
    out.extend_from_slice(&[GS, b'V', 66, 0]); // full cut
    out
}

/// Writes raw bytes to a printer/cash-drawer port. Linux POS devices usually
/// map to a raw character device (`/dev/usb/lpX`); a plain file write works
/// there. Real serial ports need the `serialport` crate opened with a baud
/// rate instead, so that's the fallback when the raw write fails.
fn write_to_port(port: &str, bytes: &[u8]) -> Result<(), String> {
    let raw_write = std::fs::OpenOptions::new()
        .write(true)
        .open(port)
        .and_then(|mut f| f.write_all(bytes));

    if raw_write.is_ok() {
        return Ok(());
    }

    let mut serial = serialport::new(port, 9600)
        .timeout(std::time::Duration::from_secs(5))
        .open()
        .map_err(|e| format!("No se pudo abrir el puerto {}: {}", port, e))?;

    serial
        .write_all(bytes)
        .map_err(|e| format!("Error al enviar datos al puerto {}: {}", port, e))
}

/// Returns the lock guarding a given port, creating it if this is the first
/// time it's used. Cloning the `Arc` and locking it from inside a spawned
/// thread is what serializes two one-shot actions that target the same
/// physical port (printer + cash drawer both write to `printer_port`, since
/// the drawer is wired through the printer's RJ11) without the frontend
/// having to coordinate anything — see "Peripheral event model" in CLAUDE.md.
fn get_port_lock(state: &AppState, port: &str) -> Arc<Mutex<()>> {
    let mut locks = state.port_locks.lock().unwrap_or_else(|e| e.into_inner());
    locks
        .entry(port.to_string())
        .or_insert_with(|| Arc::new(Mutex::new(())))
        .clone()
}

/// Fire-and-forget: dispatches the print job on a background thread and
/// returns immediately. The job's outcome is reported via
/// `peripheral-printer-status` / `peripheral-printer-error` events instead
/// of this command's return value — see "Peripheral event model" in
/// CLAUDE.md. This mirrors how the barcode scanner reports scans: the
/// frontend triggers the action and separately subscribes to its events,
/// so a print failure can never block/delay an unrelated step (like opening
/// the cash drawer) that was queued right after it — it will simply wait
/// its turn on the port lock below instead of failing with "device busy".
#[tauri::command]
pub fn trigger_print_invoice(
    payload: InvoicePrintPayload,
    printer_port: String,
    state: State<'_, AppState>,
    app_handle: AppHandle,
) -> Result<(), String> {
    if printer_port.is_empty() {
        let msg = "No hay impresora configurada".to_string();
        app_handle.emit("peripheral-printer-error", &msg).ok();
        return Err(msg);
    }

    let port_lock = get_port_lock(&state, &printer_port);

    std::thread::Builder::new()
        .name(format!("peripheral-printer-job-{}", printer_port))
        .spawn(move || {
            let bytes = build_escpos(&payload);
            let result = {
                let _guard = port_lock.lock().unwrap_or_else(|e| e.into_inner());
                write_to_port(&printer_port, &bytes)
            };
            match result {
                Ok(()) => {
                    app_handle
                        .emit(
                            "peripheral-printer-status",
                            DeviceStatusPayload { connected: true, port: printer_port },
                        )
                        .ok();
                }
                Err(e) => {
                    app_handle.emit("peripheral-printer-error", e).ok();
                }
            }
        })
        .map_err(|e| e.to_string())?;

    Ok(())
}

/// Fire-and-forget: pulses the cash-drawer kick-out line wired through the
/// printer's RJ11 port (`ESC p m t1 t2`) on a background thread. Reports via
/// `peripheral-cash-drawer-status` / `peripheral-cash-drawer-error` events —
/// same fire-and-listen shape as `trigger_print_invoice`, intentionally
/// independent of *whether printing succeeded* — but since both share the
/// same physical port, this waits on that port's lock (see `get_port_lock`)
/// so it doesn't try to open the device file while the printer job still
/// has it open. That wait is invisible to the frontend either way.
#[tauri::command]
pub fn trigger_open_cash_drawer(
    printer_port: String,
    state: State<'_, AppState>,
    app_handle: AppHandle,
) -> Result<(), String> {
    if printer_port.is_empty() {
        let msg = "No hay impresora/caja registradora configurada".to_string();
        app_handle.emit("peripheral-cash-drawer-error", &msg).ok();
        return Err(msg);
    }

    let port_lock = get_port_lock(&state, &printer_port);

    std::thread::Builder::new()
        .name(format!("peripheral-cash-drawer-job-{}", printer_port))
        .spawn(move || {
            // ESC p m t1 t2 — m=0 selects drawer pin 2 (the common kick-out pin),
            // t1/t2 are the ON/OFF pulse widths in units of 2ms (~50ms / ~500ms).
            let pulse = [ESC, b'p', 0, 25, 250];
            let result = {
                let _guard = port_lock.lock().unwrap_or_else(|e| e.into_inner());
                write_to_port(&printer_port, &pulse)
            };
            match result {
                Ok(()) => {
                    app_handle
                        .emit(
                            "peripheral-cash-drawer-status",
                            DeviceStatusPayload { connected: true, port: printer_port },
                        )
                        .ok();
                }
                Err(e) => {
                    app_handle.emit("peripheral-cash-drawer-error", e).ok();
                }
            }
        })
        .map_err(|e| e.to_string())?;

    Ok(())
}

// ── save_invoice_pdf ─────────────────────────────────────────────────────────

#[tauri::command]
pub fn save_invoice_pdf(
    base_folder: String,
    invoice_number: String,
    pdf_bytes: Vec<u8>,
    year: String,
    month: String,
    day: String,
) -> Result<String, String> {
    if base_folder.is_empty() {
        return Err("No hay carpeta de archivo configurada".to_string());
    }

    let dir = std::path::Path::new(&base_folder)
        .join(year)
        .join(month)
        .join(day);

    std::fs::create_dir_all(&dir).map_err(|e| e.to_string())?;

    let file_path = dir.join(format!("{}.pdf", invoice_number));
    std::fs::write(&file_path, pdf_bytes).map_err(|e| e.to_string())?;

    Ok(file_path.to_string_lossy().to_string())
}
