use serde::Serialize;
use tauri::State;

use crate::AppState;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PendingSaleRow {
    pub id: String,
    pub label: String,
    pub icon_key: String,
    pub items_json: String,
    pub subtotal: f64,
    pub discount: f64,
    pub total: f64,
    pub created_at: String,
}

#[tauri::command]
pub fn save_pending_sale(
    state: State<AppState>,
    id: String,
    label: String,
    icon_key: String,
    items_json: String,
    subtotal: f64,
    discount: f64,
    total: f64,
) -> Result<(), String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;
    db.conn
        .execute(
            "INSERT OR REPLACE INTO pending_sales
             (id, label, icon_key, items_json, subtotal, discount, total)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
            rusqlite::params![id, label, icon_key, items_json, subtotal, discount, total],
        )
        .map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
pub fn get_pending_sales(state: State<AppState>) -> Result<Vec<PendingSaleRow>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;
    let mut stmt = db
        .conn
        .prepare(
            "SELECT id, label, icon_key, items_json, subtotal, discount, total, created_at
             FROM pending_sales ORDER BY created_at DESC",
        )
        .map_err(|e| e.to_string())?;

    let mut rows_out = Vec::new();
    let rows = stmt
        .query_map([], |row| {
            Ok(PendingSaleRow {
                id: row.get(0)?,
                label: row.get(1)?,
                icon_key: row.get(2)?,
                items_json: row.get(3)?,
                subtotal: row.get(4)?,
                discount: row.get(5)?,
                total: row.get(6)?,
                created_at: row.get(7)?,
            })
        })
        .map_err(|e| e.to_string())?;

    for row in rows {
        rows_out.push(row.map_err(|e| e.to_string())?);
    }
    Ok(rows_out)
}

#[tauri::command]
pub fn delete_pending_sale(state: State<AppState>, id: String) -> Result<(), String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;
    db.conn
        .execute(
            "DELETE FROM pending_sales WHERE id = ?1",
            rusqlite::params![id],
        )
        .map_err(|e| e.to_string())?;
    Ok(())
}
