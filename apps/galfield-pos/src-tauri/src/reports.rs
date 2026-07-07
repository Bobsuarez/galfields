use serde::Serialize;
use tauri::State;

use crate::AppState;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SalesSummary {
    pub total_sales: f64,
    pub sale_count: i64,
    pub average_ticket: f64,
    pub items_sold: i64,
}

/// Aggregates `sales`/`sale_items` over an inclusive `[date_from, date_to]` range
/// (`'YYYY-MM-DD'` strings). Deliberately returns just one range's summary —
/// period-over-period comparison (e.g. "vs previous week") is orchestration,
/// not a single query, so it's composed in the frontend by calling this twice.
#[tauri::command]
pub fn get_sales_summary(
    state: State<AppState>,
    date_from: String,
    date_to: String,
) -> Result<SalesSummary, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let (total_sales, sale_count): (f64, i64) = db
        .conn
        .query_row(
            "SELECT COALESCE(SUM(total), 0), COUNT(*)
             FROM sales
             WHERE date(created_at) BETWEEN ?1 AND ?2",
            rusqlite::params![date_from, date_to],
            |row| Ok((row.get(0)?, row.get(1)?)),
        )
        .map_err(|e| e.to_string())?;

    let items_sold: i64 = db
        .conn
        .query_row(
            "SELECT COALESCE(SUM(si.quantity), 0)
             FROM sale_items si
             JOIN sales s ON s.id = si.sale_id
             WHERE date(s.created_at) BETWEEN ?1 AND ?2",
            rusqlite::params![date_from, date_to],
            |row| row.get(0),
        )
        .map_err(|e| e.to_string())?;

    let average_ticket = if sale_count > 0 { total_sales / sale_count as f64 } else { 0.0 };

    Ok(SalesSummary { total_sales, sale_count, average_ticket, items_sold })
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TopProductRow {
    pub rank: i64,
    pub name: String,
    pub revenue: f64,
    pub quantity: i64,
}

/// Best-selling products by revenue over an inclusive `[date_from, date_to]` range.
#[tauri::command]
pub fn get_top_products(
    state: State<AppState>,
    date_from: String,
    date_to: String,
    limit: i64,
) -> Result<Vec<TopProductRow>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let mut stmt = db
        .conn
        .prepare(
            "SELECT p.product_name, SUM(si.subtotal) AS revenue, SUM(si.quantity) AS quantity
             FROM sale_items si
             JOIN sales s ON s.id = si.sale_id
             JOIN products p ON p.id = si.product_id
             WHERE date(s.created_at) BETWEEN ?1 AND ?2
             GROUP BY si.product_id
             ORDER BY revenue DESC
             LIMIT ?3",
        )
        .map_err(|e| e.to_string())?;

    let rows = stmt
        .query_map(rusqlite::params![date_from, date_to, limit], |row| {
            Ok((row.get::<_, String>(0)?, row.get::<_, f64>(1)?, row.get::<_, i64>(2)?))
        })
        .map_err(|e| e.to_string())?;

    let mut products = Vec::new();
    for (rank, row) in rows.enumerate() {
        let (name, revenue, quantity) = row.map_err(|e| e.to_string())?;
        products.push(TopProductRow { rank: rank as i64 + 1, name, revenue, quantity });
    }

    Ok(products)
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct FinancialSummary {
    pub gross_sales: f64,
    pub discounts: f64,
    pub net_sales: f64,
}

/// Gross/discount/net sales over an inclusive `[date_from, date_to]` range.
/// Cost-of-goods/margin aren't included here — `products` has no cost column
/// yet, so that stays out of scope until one exists.
#[tauri::command]
pub fn get_financial_summary(
    state: State<AppState>,
    date_from: String,
    date_to: String,
) -> Result<FinancialSummary, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let (gross_sales, discounts, net_sales): (f64, f64, f64) = db
        .conn
        .query_row(
            "SELECT COALESCE(SUM(subtotal), 0), COALESCE(SUM(discount), 0), COALESCE(SUM(total), 0)
             FROM sales
             WHERE date(created_at) BETWEEN ?1 AND ?2",
            rusqlite::params![date_from, date_to],
            |row| Ok((row.get(0)?, row.get(1)?, row.get(2)?)),
        )
        .map_err(|e| e.to_string())?;

    Ok(FinancialSummary { gross_sales, discounts, net_sales })
}
