use serde::Serialize;
use tauri::State;

use crate::AppState;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LowStockRow {
    pub product_name: String,
    pub current_stock: f64,
}

/// Mirrors `Product` in `src/types/index.ts` field-for-field (including
/// coercing nullable `barcode`/`category` columns to `""` and `id` to a
/// string) so the frontend needs no mapping layer between this and the mock
/// data it replaces.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ProductRow {
    pub id: String,
    pub barcode: String,
    pub product_name: String,
    pub unit_price: f64,
    pub category: String,
    pub is_active: bool,
    pub image_path: String,
    pub image_hash: String,
    pub stock_quantity: f64,
    pub last_sync_at: Option<String>,
    pub created_at: String,
    pub updated_at: String,
}

/// All local products (synced from the cloud catalog via `sync_products`, or
/// none yet if the user hasn't synced this install). Powers both the POS
/// catalog (active only, filtered client-side) and the Inventory list.
#[tauri::command]
pub fn get_products(state: State<AppState>) -> Result<Vec<ProductRow>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let mut stmt = db
        .conn
        .prepare(
            "SELECT id, barcode, product_name, unit_price, category, is_active,
                    image_path, image_hash, stock_quantity, last_sync_at, created_at, updated_at
             FROM products
             ORDER BY product_name ASC",
        )
        .map_err(|e| e.to_string())?;

    let rows = stmt
        .query_map([], |row| {
            let id: i64 = row.get(0)?;
            let is_active: i64 = row.get(5)?;
            Ok(ProductRow {
                id: id.to_string(),
                barcode: row.get::<_, Option<String>>(1)?.unwrap_or_default(),
                product_name: row.get(2)?,
                unit_price: row.get(3)?,
                category: row.get::<_, Option<String>>(4)?.unwrap_or_default(),
                is_active: is_active != 0,
                image_path: row.get(6)?,
                image_hash: row.get(7)?,
                stock_quantity: row.get(8)?,
                last_sync_at: row.get(9)?,
                created_at: row.get(10)?,
                updated_at: row.get(11)?,
            })
        })
        .map_err(|e| e.to_string())?;

    rows.collect::<Result<Vec<_>, _>>().map_err(|e| e.to_string())
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
