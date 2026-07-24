//! Browsing and cancelling past sales — the one write path besides
//! `invoices::create_sale` that ever touches `sales`. There is no pagination
//! here on purpose (last 200, optionally filtered by invoice number) — this
//! is a deliberately small first version of "view past sales," which didn't
//! exist at all before this module; revisit if sales volume ever makes 200
//! too few to find a specific one.

use serde::Serialize;
use tauri::State;

use crate::http_client;
use crate::logging;
use crate::AppState;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SaleListRow {
    pub sale_id: i64,
    pub invoice_number: String,
    pub created_at: String,
    pub total: f64,
    pub payment_method_name: String,
    pub cancelled_at: Option<String>,
}

#[tauri::command]
pub fn list_sales(state: State<AppState>, search: Option<String>) -> Result<Vec<SaleListRow>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;
    let pattern = format!("%{}%", search.unwrap_or_default());

    let mut stmt = db
        .conn
        .prepare(
            "SELECT s.id, s.invoice_number, s.created_at, s.total, pm.name, s.cancelled_at
             FROM sales s
             JOIN payment_method pm ON pm.id = s.payment_method
             WHERE COALESCE(s.invoice_number, '') LIKE ?1
             ORDER BY s.created_at DESC, s.id DESC
             LIMIT 200",
        )
        .map_err(|e| e.to_string())?;

    let rows = stmt
        .query_map(rusqlite::params![pattern], |row| {
            Ok(SaleListRow {
                sale_id: row.get(0)?,
                invoice_number: row.get::<_, Option<String>>(1)?.unwrap_or_default(),
                created_at: row.get(2)?,
                total: row.get(3)?,
                payment_method_name: row.get(4)?,
                cancelled_at: row.get(5)?,
            })
        })
        .map_err(|e| e.to_string())?
        .collect::<Result<Vec<_>, _>>()
        .map_err(|e| e.to_string())?;

    Ok(rows)
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SaleDetailItem {
    pub product_name: String,
    pub quantity: i64,
    pub unit_price: f64,
    pub subtotal: f64,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SaleDetail {
    pub sale_id: i64,
    pub invoice_number: String,
    pub created_at: String,
    pub subtotal: f64,
    pub discount: f64,
    pub total: f64,
    pub payment_method_name: String,
    pub cancelled_at: Option<String>,
    pub items: Vec<SaleDetailItem>,
}

#[tauri::command]
pub fn get_sale_detail(state: State<AppState>, sale_id: i64) -> Result<SaleDetail, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let (invoice_number, created_at, subtotal, discount, total, payment_method_name, cancelled_at) = db
        .conn
        .query_row(
            "SELECT s.invoice_number, s.created_at, s.subtotal, s.discount, s.total, pm.name, s.cancelled_at
             FROM sales s
             JOIN payment_method pm ON pm.id = s.payment_method
             WHERE s.id = ?1",
            rusqlite::params![sale_id],
            |row| {
                Ok((
                    row.get::<_, Option<String>>(0)?.unwrap_or_default(),
                    row.get::<_, String>(1)?,
                    row.get::<_, f64>(2)?,
                    row.get::<_, f64>(3)?,
                    row.get::<_, f64>(4)?,
                    row.get::<_, String>(5)?,
                    row.get::<_, Option<String>>(6)?,
                ))
            },
        )
        .map_err(|_| format!("Venta {sale_id} no encontrada"))?;

    let mut stmt = db
        .conn
        .prepare(
            "SELECT p.product_name, si.quantity, si.unit_price, si.subtotal
             FROM sale_items si
             JOIN products p ON p.id = si.product_id
             WHERE si.sale_id = ?1",
        )
        .map_err(|e| e.to_string())?;

    let items = stmt
        .query_map(rusqlite::params![sale_id], |row| {
            Ok(SaleDetailItem {
                product_name: row.get(0)?,
                quantity: row.get(1)?,
                unit_price: row.get(2)?,
                subtotal: row.get(3)?,
            })
        })
        .map_err(|e| e.to_string())?
        .collect::<Result<Vec<_>, _>>()
        .map_err(|e| e.to_string())?;

    Ok(SaleDetail {
        sale_id,
        invoice_number,
        created_at,
        subtotal,
        discount,
        total,
        payment_method_name,
        cancelled_at,
        items,
    })
}

/// Reverses the stock decrement `create_sale` applied, symmetrically (same
/// `products.stock_quantity` column, opposite sign). If this sale was already
/// pushed to the cloud (`synced_at` set), the cloud side is cancelled FIRST,
/// synchronously — if that fails (no connection, server error), this returns
/// early without touching anything locally, so a fiscal document never ends
/// up cancelled on one side and not the other. A sale that was cancelled
/// before ever syncing just gets cancelled locally; `sales_sync.rs` never
/// pushes it (see the `cancelled_at IS NULL` filter there).
#[tauri::command]
pub async fn cancel_sale(state: State<'_, AppState>, sale_id: i64) -> Result<(), String> {
    const LOC: &str = "sale_history::cancel_sale";

    let (sync_uuid, synced_at, cancelled_at, items, api_base_url) = {
        let db = state.db.lock().map_err(|e| e.to_string())?;

        let (sync_uuid, synced_at, cancelled_at): (Option<String>, Option<String>, Option<String>) = db
            .conn
            .query_row(
                "SELECT sync_uuid, synced_at, cancelled_at FROM sales WHERE id = ?1",
                rusqlite::params![sale_id],
                |row| Ok((row.get(0)?, row.get(1)?, row.get(2)?)),
            )
            .map_err(|_| format!("Venta {sale_id} no encontrada"))?;

        let mut stmt = db
            .conn
            .prepare("SELECT product_id, quantity FROM sale_items WHERE sale_id = ?1")
            .map_err(|e| e.to_string())?;
        let items: Vec<(i64, f64)> = stmt
            .query_map(rusqlite::params![sale_id], |row| {
                Ok((row.get::<_, i64>(0)?, row.get::<_, f64>(1)?))
            })
            .map_err(|e| e.to_string())?
            .collect::<Result<Vec<_>, _>>()
            .map_err(|e| e.to_string())?;

        (sync_uuid, synced_at, cancelled_at, items, http_client::api_base_url(&db))
    };

    if cancelled_at.is_some() {
        return Err("Esta factura ya está cancelada.".to_string());
    }

    if synced_at.is_some() {
        let sync_uuid = sync_uuid
            .ok_or_else(|| "Venta sincronizada sin sync_uuid — datos locales inconsistentes.".to_string())?;

        logging::step(LOC, format!("venta {sale_id} ya sincronizada, cancelando en la nube primero"));

        let response = http_client::post(&format!(
            "{}/api/sales/by-client-event/{}/cancel",
            api_base_url, sync_uuid
        ))
        .await?;

        if !response.is_success() {
            let msg = format!(
                "No se pudo cancelar en la nube (respuesta {}). Verifica tu conexión e intenta de nuevo.",
                response.status
            );
            logging::step(LOC, format!("cancelación en la nube falló: {}", msg));
            return Err(msg);
        }
    }

    let mut db = state.db.lock().map_err(|e| e.to_string())?;
    let tx = db.conn.transaction().map_err(|e| e.to_string())?;

    tx.execute(
        "UPDATE sales SET cancelled_at = datetime('now', 'localtime') WHERE id = ?1",
        rusqlite::params![sale_id],
    )
    .map_err(|e| e.to_string())?;

    for (product_id, quantity) in &items {
        tx.execute(
            "UPDATE products SET stock_quantity = stock_quantity + ?1 WHERE id = ?2",
            rusqlite::params![quantity, product_id],
        )
        .map_err(|e| e.to_string())?;
    }

    tx.commit().map_err(|e| e.to_string())?;
    logging::step(LOC, format!("venta {sale_id} cancelada, stock repuesto"));

    Ok(())
}
