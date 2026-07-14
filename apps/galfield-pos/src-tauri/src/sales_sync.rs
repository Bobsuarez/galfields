use serde::Serialize;
use std::time::Duration;
use tauri::{AppHandle, Emitter, Manager, State};

use crate::http_client::{self, API_BASE_URL};
use crate::logging;
use crate::AppState;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct SaleItemPayload {
    variant_id: i64,
    quantity: i64,
    unit_price: f64,
    subtotal: f64,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct SalePaymentPayload {
    payment_method_id: i64,
    amount: f64,
}

/// Mirrors backend/pos's `SaleRequest` — one call per sale, reporting the
/// whole thing (items + payment + totals) instead of just a stock delta.
/// The backend applies the matching stock adjustment atomically under this
/// same `client_event_id` (see that repo's CLAUDE.md, "Sale recording
/// endpoint").
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct SaleRequestPayload {
    client_event_id: String,
    items: Vec<SaleItemPayload>,
    payments: Vec<SalePaymentPayload>,
    discount_amount: f64,
    total_amount: f64,
}

/// Mirrors `sales-sync-status` payload shape sent to the frontend after
/// every sweep (manual or scheduled), so `AppStatusbar.vue` can show a
/// "N ventas pendientes" indicator without polling.
#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct SalesSyncStatusPayload {
    pub pending_count: i64,
    pub pushed_count: i64,
    pub error: Option<String>,
}

struct PendingSaleItem {
    /// `None` when the local product predates `sync.rs`'s remote-linking
    /// migration - see the "unreportable" check in `push_pending_sales`.
    remote_variant_id: Option<i64>,
    quantity: i64,
    unit_price: f64,
    subtotal: f64,
}

struct PendingSale {
    sale_id: i64,
    sync_uuid: String,
    discount: f64,
    total: f64,
    /// `None` when the sale's payment method predates
    /// `catalog_sync.rs`'s remote-linking migration (006).
    remote_payment_method_id: Option<i64>,
    items: Vec<PendingSaleItem>,
}

fn mark_synced(state: &State<'_, AppState>, sale_id: i64) -> Result<(), String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;
    db.conn
        .execute(
            "UPDATE sales SET synced_at = datetime('now', 'localtime') WHERE id = ?1",
            rusqlite::params![sale_id],
        )
        .map_err(|e| e.to_string())?;
    Ok(())
}

fn load_pending_sales(state: &State<'_, AppState>) -> Result<Vec<PendingSale>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let mut sales_stmt = db
        .conn
        .prepare(
            "SELECT s.id, s.sync_uuid, s.discount, s.total, pm.remote_payment_method_id
             FROM sales s
             JOIN payment_method pm ON pm.id = s.payment_method
             WHERE s.synced_at IS NULL
             ORDER BY s.id ASC",
        )
        .map_err(|e| e.to_string())?;

    let sale_rows: Vec<(i64, String, f64, f64, Option<i64>)> = sales_stmt
        .query_map([], |row| {
            Ok((row.get(0)?, row.get(1)?, row.get(2)?, row.get(3)?, row.get(4)?))
        })
        .map_err(|e| e.to_string())?
        .collect::<Result<Vec<_>, _>>()
        .map_err(|e| e.to_string())?;

    let mut items_stmt = db
        .conn
        .prepare(
            "SELECT p.remote_variant_id, si.quantity, si.unit_price, si.subtotal
             FROM sale_items si
             JOIN products p ON p.id = si.product_id
             WHERE si.sale_id = ?1",
        )
        .map_err(|e| e.to_string())?;

    let mut pending = Vec::new();
    for (sale_id, sync_uuid, discount, total, remote_payment_method_id) in sale_rows {
        let items: Vec<PendingSaleItem> = items_stmt
            .query_map(rusqlite::params![sale_id], |row| {
                Ok(PendingSaleItem {
                    remote_variant_id: row.get(0)?,
                    quantity: row.get(1)?,
                    unit_price: row.get(2)?,
                    subtotal: row.get(3)?,
                })
            })
            .map_err(|e| e.to_string())?
            .collect::<Result<Vec<_>, _>>()
            .map_err(|e| e.to_string())?;
        pending.push(PendingSale {
            sale_id,
            sync_uuid,
            discount,
            total,
            remote_payment_method_id,
            items,
        });
    }

    Ok(pending)
}

/// Pushes every pending sale (`synced_at IS NULL`) to the cloud, one HTTP
/// call per sale (`POST /api/sales`), oldest first. Stops at the first
/// *network/server* failure instead of skipping ahead, so a genuinely
/// offline terminal doesn't hammer the network retrying every pending sale,
/// and sales stay in creation order once connectivity returns.
///
/// Sales with no line items at all are marked synced immediately without a
/// network call: there's nothing the cloud can ever do with them.
///
/// Sales that have items but aren't fully "linked" to the cloud yet (any
/// product missing `remote_variant_id`, or the payment method missing
/// `remote_payment_method_id` — both only populated by their respective
/// catalog syncs) are **skipped, not stopped on** — left pending, but the
/// loop moves on to the next sale instead of blocking the whole queue behind
/// a purely local data gap that a catalog re-sync would resolve. Sending a
/// partial sale (dropping unlinked lines) was the old stock-only endpoint's
/// behavior; it's wrong here because the reported total/items would no
/// longer match.
///
/// Called two ways: fire-and-forget right after `create_sale` (POSView's
/// checkout flow), and from the scheduled background loop in `lib.rs` at
/// `sync.sales_retry_minutes` — never blocks checkout on network state.
#[tauri::command]
pub async fn push_pending_sales(app: AppHandle, state: State<'_, AppState>) -> Result<SalesSyncStatusPayload, String> {
    const LOC: &str = "sales_sync::push_pending_sales";
    let mut pending = load_pending_sales(&state)?;
    logging::step(LOC, format!("{} ventas pendientes encontradas", pending.len()));

    // Nothing to report to the cloud, ever - resolve locally so they don't
    // sit in the pending count forever.
    pending.retain(|sale| {
        if sale.items.is_empty() {
            let _ = mark_synced(&state, sale.sale_id);
            false
        } else {
            true
        }
    });

    let mut pushed_count: i64 = 0;
    let mut error: Option<String> = None;

    for sale in &pending {
        let all_items_linked = sale.items.iter().all(|i| i.remote_variant_id.is_some());

        let Some(remote_payment_method_id) = sale.remote_payment_method_id else {
            logging::step(LOC, format!("venta {} omitida: método de pago sin vincular con la nube (pendiente hasta el próximo sync de catálogo)", sale.sale_id));
            continue;
        };
        if !all_items_linked {
            logging::step(LOC, format!("venta {} omitida: uno o más productos sin vincular con la nube (pendiente hasta el próximo sync de catálogo)", sale.sale_id));
            continue;
        }

        let payload = SaleRequestPayload {
            client_event_id: sale.sync_uuid.clone(),
            items: sale
                .items
                .iter()
                .map(|item| SaleItemPayload {
                    variant_id: item.remote_variant_id.expect("checked above"),
                    quantity: item.quantity,
                    unit_price: item.unit_price,
                    subtotal: item.subtotal,
                })
                .collect(),
            payments: vec![SalePaymentPayload {
                payment_method_id: remote_payment_method_id,
                amount: sale.total,
            }],
            discount_amount: sale.discount,
            total_amount: sale.total,
        };

        let response = http_client::post_json(&format!("{}/api/sales", API_BASE_URL), &payload).await;

        match response {
            Ok(res) if res.is_success() => {
                mark_synced(&state, sale.sale_id)?;
                pushed_count += 1;
                logging::step(LOC, format!("venta {} reportada", sale.sale_id));
            }
            Ok(res) => {
                error = Some(format!("El servidor respondió {} al reportar una venta", res.status));
                logging::step(LOC, format!("venta {} rechazada por el servidor: {}", sale.sale_id, res.status));
                break;
            }
            Err(e) => {
                logging::step(LOC, format!("venta {} no se pudo reportar: {}", sale.sale_id, e));
                error = Some(e);
                break;
            }
        }
    }

    let pending_count = load_pending_sales(&state)?.len() as i64;
    logging::step(LOC, format!("terminado: {} enviadas, {} pendientes restantes", pushed_count, pending_count));

    let status = SalesSyncStatusPayload { pending_count, pushed_count, error };
    let _ = app.emit("sales-sync-status", status.clone());

    Ok(status)
}

/// Started once from `lib.rs`'s `.setup()` — pushes immediately (catches up
/// on anything left pending from a previous offline session), then sleeps
/// for `sync.sales_retry_minutes` and repeats. Re-reads that setting from
/// `app_settings` on every wake (not just once at startup), so changing it
/// in Configuración takes effect without restarting the app — same reason
/// the interval isn't just a fixed Rust constant.
pub fn spawn_background_retry_loop(app: AppHandle) {
    tauri::async_runtime::spawn(async move {
        loop {
            let state = app.state::<AppState>();
            if let Err(e) = push_pending_sales(app.clone(), state).await {
                eprintln!("[sales-sync] background retry failed: {}", e);
            }

            let interval_minutes: u64 = {
                let state = app.state::<AppState>();
                let Ok(db) = state.db.lock() else { return };
                db.conn
                    .query_row(
                        "SELECT value_property FROM app_settings WHERE key_property = 'sync.sales_retry_minutes'",
                        [],
                        |row| row.get::<_, String>(0),
                    )
                    .ok()
                    .and_then(|v| v.parse::<u64>().ok())
                    .unwrap_or(5)
            };

            tokio::time::sleep(Duration::from_secs(interval_minutes.max(1) * 60)).await;
        }
    });
}
