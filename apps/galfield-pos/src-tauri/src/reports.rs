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

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DailySalesRow {
    pub date:  String,
    pub total: f64,
}

/// Sales total per calendar day over an inclusive `[date_from, date_to]` range.
/// Every day in the range gets a row (0 when there were no sales that day) via
/// a recursive date-series CTE, so the frontend line chart never has to guess
/// at gaps — "vs previous period" is composed by calling this twice, same
/// pattern as `get_sales_summary`.
#[tauri::command]
pub fn get_sales_by_day(
    state: State<AppState>,
    date_from: String,
    date_to: String,
) -> Result<Vec<DailySalesRow>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let mut stmt = db
        .conn
        .prepare(
            "WITH RECURSIVE dates(d) AS (
                SELECT date(?1)
                UNION ALL
                SELECT date(d, '+1 day') FROM dates WHERE d < date(?2)
             )
             SELECT dates.d, COALESCE(SUM(s.total), 0)
             FROM dates
             LEFT JOIN sales s ON date(s.created_at) = dates.d
             GROUP BY dates.d
             ORDER BY dates.d",
        )
        .map_err(|e| e.to_string())?;

    let rows = stmt
        .query_map(rusqlite::params![date_from, date_to], |row| {
            Ok(DailySalesRow { date: row.get(0)?, total: row.get(1)? })
        })
        .map_err(|e| e.to_string())?;

    rows.collect::<Result<Vec<_>, _>>().map_err(|e| e.to_string())
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CategorySalesRow {
    pub category: String,
    pub total:    f64,
}

/// Revenue per product category over an inclusive `[date_from, date_to]`
/// range. `products.category` is a plain denormalized text column (see
/// CLAUDE.md), so products with an empty/NULL category are grouped under
/// "Sin categoría" instead of being dropped.
#[tauri::command]
pub fn get_sales_by_category(
    state: State<AppState>,
    date_from: String,
    date_to: String,
) -> Result<Vec<CategorySalesRow>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let mut stmt = db
        .conn
        .prepare(
            "SELECT COALESCE(NULLIF(p.category, ''), 'Sin categoría') AS category,
                    SUM(si.subtotal) AS total
             FROM sale_items si
             JOIN sales s ON s.id = si.sale_id
             JOIN products p ON p.id = si.product_id
             WHERE date(s.created_at) BETWEEN ?1 AND ?2
             GROUP BY category
             ORDER BY total DESC",
        )
        .map_err(|e| e.to_string())?;

    let rows = stmt
        .query_map(rusqlite::params![date_from, date_to], |row| {
            Ok(CategorySalesRow { category: row.get(0)?, total: row.get(1)? })
        })
        .map_err(|e| e.to_string())?;

    rows.collect::<Result<Vec<_>, _>>().map_err(|e| e.to_string())
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HourlySalesRow {
    pub hour:  String,
    pub total: f64,
}

/// Sales total grouped by hour-of-day (`00`-`23`) over an inclusive
/// `[date_from, date_to]` range. Only hours that actually had a sale are
/// returned (no zero-padding across all 24 hours) — the bar chart just
/// renders whatever comes back.
#[tauri::command]
pub fn get_sales_by_hour(
    state: State<AppState>,
    date_from: String,
    date_to: String,
) -> Result<Vec<HourlySalesRow>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let mut stmt = db
        .conn
        .prepare(
            "SELECT strftime('%H', created_at) AS hour, SUM(total) AS total
             FROM sales
             WHERE date(created_at) BETWEEN ?1 AND ?2
             GROUP BY hour
             ORDER BY hour",
        )
        .map_err(|e| e.to_string())?;

    let rows = stmt
        .query_map(rusqlite::params![date_from, date_to], |row| {
            Ok(HourlySalesRow { hour: row.get(0)?, total: row.get(1)? })
        })
        .map_err(|e| e.to_string())?;

    rows.collect::<Result<Vec<_>, _>>().map_err(|e| e.to_string())
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PaymentMethodSalesRow {
    pub method:     String,
    pub total:      f64,
    pub sale_count: i64,
}

/// Revenue and sale count per payment method over an inclusive
/// `[date_from, date_to]` range.
#[tauri::command]
pub fn get_sales_by_payment_method(
    state: State<AppState>,
    date_from: String,
    date_to: String,
) -> Result<Vec<PaymentMethodSalesRow>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let mut stmt = db
        .conn
        .prepare(
            "SELECT pm.name, SUM(s.total) AS total, COUNT(*) AS sale_count
             FROM sales s
             JOIN payment_method pm ON pm.id = s.payment_method
             WHERE date(s.created_at) BETWEEN ?1 AND ?2
             GROUP BY pm.id
             ORDER BY total DESC",
        )
        .map_err(|e| e.to_string())?;

    let rows = stmt
        .query_map(rusqlite::params![date_from, date_to], |row| {
            Ok(PaymentMethodSalesRow {
                method:     row.get(0)?,
                total:      row.get(1)?,
                sale_count: row.get(2)?,
            })
        })
        .map_err(|e| e.to_string())?;

    rows.collect::<Result<Vec<_>, _>>().map_err(|e| e.to_string())
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ProductNoMovementRow {
    pub product_name:   String,
    pub category:       String,
    pub stock_quantity: f64,
}

/// Active products with zero `sale_items` in the inclusive `[date_from,
/// date_to]` range — dead-stock candidates. Capped by `limit` (ordered
/// alphabetically, there's no natural "worst first" order for zero sales).
#[tauri::command]
pub fn get_products_without_movement(
    state: State<AppState>,
    date_from: String,
    date_to: String,
    limit: i64,
) -> Result<Vec<ProductNoMovementRow>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let mut stmt = db
        .conn
        .prepare(
            "SELECT p.product_name, COALESCE(p.category, ''), p.stock_quantity
             FROM products p
             WHERE p.is_active = 1
               AND NOT EXISTS (
                 SELECT 1 FROM sale_items si
                 JOIN sales s ON s.id = si.sale_id
                 WHERE si.product_id = p.id AND date(s.created_at) BETWEEN ?1 AND ?2
               )
             ORDER BY p.product_name
             LIMIT ?3",
        )
        .map_err(|e| e.to_string())?;

    let rows = stmt
        .query_map(rusqlite::params![date_from, date_to, limit], |row| {
            Ok(ProductNoMovementRow {
                product_name:   row.get(0)?,
                category:       row.get(1)?,
                stock_quantity: row.get(2)?,
            })
        })
        .map_err(|e| e.to_string())?;

    rows.collect::<Result<Vec<_>, _>>().map_err(|e| e.to_string())
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LowStockReportRow {
    pub product_name:   String,
    pub current_stock:  f64,
    pub threshold:      f64,
    pub units_sold:     i64,
    /// "critico" when stock is at or below 1 unit, "bajo" otherwise —
    /// mirrors the cutoff `AppSidebar.vue` already uses for its low-stock
    /// alert list, kept consistent rather than inventing a second rule.
    pub status:         String,
}

/// Low-stock active products (`stock_quantity <= threshold`), each paired
/// with how many units sold in the inclusive `[date_from, date_to]` range —
/// unlike `products::get_low_stock_products` (used by the sidebar banner,
/// which has no date range and no sales figure), this is the reports-page
/// version with the extra "Ventas" column the report table needs.
#[tauri::command]
pub fn get_low_stock_report(
    state: State<AppState>,
    date_from: String,
    date_to: String,
    threshold: f64,
    limit: i64,
) -> Result<Vec<LowStockReportRow>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let mut stmt = db
        .conn
        .prepare(
            "SELECT p.product_name, p.stock_quantity,
                    COALESCE((
                      SELECT SUM(si.quantity)
                      FROM sale_items si
                      JOIN sales s ON s.id = si.sale_id
                      WHERE si.product_id = p.id AND date(s.created_at) BETWEEN ?1 AND ?2
                    ), 0) AS units_sold
             FROM products p
             WHERE p.is_active = 1 AND p.stock_quantity <= ?3
             ORDER BY p.stock_quantity ASC
             LIMIT ?4",
        )
        .map_err(|e| e.to_string())?;

    let rows = stmt
        .query_map(rusqlite::params![date_from, date_to, threshold, limit], |row| {
            let current_stock: f64 = row.get(1)?;
            Ok(LowStockReportRow {
                product_name:  row.get(0)?,
                current_stock,
                threshold,
                units_sold:    row.get(2)?,
                status:        if current_stock <= 1.0 { "critico".to_string() } else { "bajo".to_string() },
            })
        })
        .map_err(|e| e.to_string())?;

    rows.collect::<Result<Vec<_>, _>>().map_err(|e| e.to_string())
}
