use serde::Serialize;
use tauri::State;

use crate::AppState;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PaymentMethodRow {
    pub id: i64,
    pub name: String,
}

/// Only `is_active = 1` rows — a payment method deactivated from the cloud
/// (see `catalog_sync::sync_payment_methods`) stops being offered for new
/// sales, but existing `sales.payment_method` rows keep their FK regardless
/// (reports read those by id, never through this list).
#[tauri::command]
pub fn list_payment_methods(state: State<AppState>) -> Result<Vec<PaymentMethodRow>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;
    let mut stmt = db
        .conn
        .prepare("SELECT id, name FROM payment_method WHERE is_active = 1 ORDER BY id")
        .map_err(|e| e.to_string())?;

    let rows = stmt
        .query_map([], |row| {
            Ok(PaymentMethodRow { id: row.get(0)?, name: row.get(1)? })
        })
        .map_err(|e| e.to_string())?;

    let mut out = Vec::new();
    for row in rows {
        out.push(row.map_err(|e| e.to_string())?);
    }
    Ok(out)
}
