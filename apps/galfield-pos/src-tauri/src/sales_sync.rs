use serde::Serialize;
use std::time::Duration;
use tauri::{AppHandle, Emitter, Manager, State};

use crate::http_client::{self, API_BASE_URL};
use crate::logging;
use crate::AppState;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct StockAdjustmentItem {
    variant_id: i64,
    quantity_delta: i64,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct StockAdjustmentBatchRequest {
    client_event_id: String,
    items: Vec<StockAdjustmentItem>,
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

struct PendingSale {
    sale_id: i64,
    sync_uuid: String,
    /// (remote_variant_id, quantity) - only lines whose local product has a
    /// `remote_variant_id` (i.e. came from a real cloud sync) are reportable.
    items: Vec<(i64, i64)>,
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
        .prepare("SELECT id, sync_uuid FROM sales WHERE synced_at IS NULL ORDER BY id ASC")
        .map_err(|e| e.to_string())?;

    let sale_rows: Vec<(i64, String)> = sales_stmt
        .query_map([], |row| Ok((row.get(0)?, row.get(1)?)))
        .map_err(|e| e.to_string())?
        .collect::<Result<Vec<_>, _>>()
        .map_err(|e| e.to_string())?;

    let mut items_stmt = db
        .conn
        .prepare(
            "SELECT p.remote_variant_id, si.quantity
             FROM sale_items si
             JOIN products p ON p.id = si.product_id
             WHERE si.sale_id = ?1 AND p.remote_variant_id IS NOT NULL",
        )
        .map_err(|e| e.to_string())?;

    let mut pending = Vec::new();
    for (sale_id, sync_uuid) in sale_rows {
        let items: Vec<(i64, i64)> = items_stmt
            .query_map(rusqlite::params![sale_id], |row| Ok((row.get(0)?, row.get(1)?)))
            .map_err(|e| e.to_string())?
            .collect::<Result<Vec<_>, _>>()
            .map_err(|e| e.to_string())?;
        pending.push(PendingSale { sale_id, sync_uuid, items });
    }

    Ok(pending)
}

/// Pushes every pending sale (`synced_at IS NULL`) to the cloud, one HTTP
/// call per sale (`POST /api/inventory/adjustments`), oldest first. Stops at
/// the first failure instead of skipping ahead, so a genuinely offline
/// terminal doesn't hammer the network retrying every pending sale, and
/// sales stay in creation order once connectivity returns.
///
/// Sales with no mappable line items (every product predates the
/// `remote_variant_id` link - see `sync.rs`) are marked synced immediately
/// without a network call: there's nothing the cloud can do with them, and
/// leaving them pending forever would never resolve.
///
/// Called two ways: fire-and-forget right after `create_sale` (POSView's
/// checkout flow), and from the scheduled background loop in `lib.rs` at
/// `sync.sales_retry_minutes` — never blocks checkout on network state.
#[tauri::command]
pub async fn push_pending_sales(app: AppHandle, state: State<'_, AppState>) -> Result<SalesSyncStatusPayload, String> {
    const LOC: &str = "sales_sync::push_pending_sales";
    let mut pending = load_pending_sales(&state)?;
    logging::step(LOC, format!("{} ventas pendientes encontradas", pending.len()));

    // Nothing to report to the cloud - resolve locally so they don't sit in
    // the pending count forever.
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
        let payload = StockAdjustmentBatchRequest {
            client_event_id: sale.sync_uuid.clone(),
            items: sale
                .items
                .iter()
                .map(|(variant_id, quantity)| StockAdjustmentItem {
                    variant_id: *variant_id,
                    quantity_delta: -quantity,
                })
                .collect(),
        };

        let response = http_client::post_json(&format!("{}/api/inventory/adjustments", API_BASE_URL), &payload).await;

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
