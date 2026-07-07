use serde::Serialize;
use tauri::State;

use crate::AppState;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LowStockRow {
    pub product_name: String,
    pub current_stock: f64,
}

/// Active products at or below `threshold` units, lowest stock first.
/// `products` has no per-product `min_stock` column yet, so the threshold is
/// a single value the caller picks (e.g. a fixed constant) rather than a
/// per-row comparison — swap for a real `min_stock` column once one exists.
#[tauri::command]
pub fn get_low_stock_products(
    state: State<AppState>,
    threshold: f64,
    limit: i64,
) -> Result<Vec<LowStockRow>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let mut stmt = db
        .conn
        .prepare(
            "SELECT product_name, stock_quantity
             FROM products
             WHERE is_active = 1 AND stock_quantity <= ?1
             ORDER BY stock_quantity ASC
             LIMIT ?2",
        )
        .map_err(|e| e.to_string())?;

    let rows = stmt
        .query_map(rusqlite::params![threshold, limit], |row| {
            Ok(LowStockRow {
                product_name: row.get(0)?,
                current_stock: row.get(1)?,
            })
        })
        .map_err(|e| e.to_string())?;

    rows.collect::<Result<Vec<_>, _>>().map_err(|e| e.to_string())
}
