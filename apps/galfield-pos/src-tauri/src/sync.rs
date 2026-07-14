use serde::{Deserialize, Serialize};
use tauri::State;

use crate::http_client::API_BASE_URL;
use crate::logging;
use crate::AppState;

const PAGE_SIZE: u32 = 100;

#[derive(Deserialize)]
struct RemoteAttribute {
    value: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct RemoteVariant {
    variant_id: i64,
    barcode: String,
    price: f64,
    #[serde(default)]
    stock: Option<i64>,
    #[serde(default)]
    image_url: Option<String>,
    active: bool,
    #[serde(default)]
    attributes: Vec<RemoteAttribute>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct RemoteProduct {
    name: String,
    #[serde(default)]
    category_name: Option<String>,
    #[serde(default)]
    image_url: Option<String>,
    active: bool,
    #[serde(default)]
    variants: Vec<RemoteVariant>,
}

// Spring's PagedModel (`@EnableSpringDataWebSupport(pageSerializationMode =
// VIA_DTO)`, backend/pos's PosApplication.java) nests pagination metadata
// under `page` instead of the flat `last`/`totalElements` fields older Page
// serialization used - matching this exactly is what broke with "missing
// field `last`" before this shape was matched.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct RemotePageMeta {
    number: u32,
    total_pages: u32,
}

#[derive(Deserialize)]
struct RemotePage {
    content: Vec<RemoteProduct>,
    page: RemotePageMeta,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SyncSummary {
    pub products_fetched: i64,
    pub variants_synced: i64,
    /// Local rows not seen in this run (deleted or unpublished in the cloud
    /// feed) and therefore marked inactive — see the "stale" sweep below.
    pub products_deactivated: i64,
}

/// One local `products` row per cloud *variant*, not per cloud product: the
/// local schema is deliberately flat (see 001_initial.sql) - a single
/// barcode/price/stock per row - and checkout scans against a specific
/// variant's own barcode, so every variant needs its own row to be
/// scannable, not just the product's first one.
struct LocalProductRow {
    remote_variant_id: i64,
    barcode: String,
    product_name: String,
    unit_price: f64,
    category: String,
    image_path: String,
    stock_quantity: f64,
    is_active: bool,
}

fn flatten(product: RemoteProduct) -> Vec<LocalProductRow> {
    let multi_variant = product.variants.len() > 1;

    product
        .variants
        .into_iter()
        .filter(|v| !v.barcode.trim().is_empty())
        .map(|variant| {
            // Multi-variant products need the variant called out in the
            // name (e.g. "Camiseta Basica - Rojo M") since the local table
            // has no separate variant/attribute concept to disambiguate them.
            let product_name = if multi_variant {
                let attrs = variant
                    .attributes
                    .iter()
                    .map(|a| a.value.as_str())
                    .collect::<Vec<_>>()
                    .join(" ");
                if attrs.is_empty() {
                    product.name.clone()
                } else {
                    format!("{} - {}", product.name, attrs)
                }
            } else {
                product.name.clone()
            };

            LocalProductRow {
                remote_variant_id: variant.variant_id,
                barcode: variant.barcode,
                product_name,
                unit_price: variant.price,
                category: product.category_name.clone().unwrap_or_default(),
                image_path: variant
                    .image_url
                    .or_else(|| product.image_url.clone())
                    .unwrap_or_default(),
                stock_quantity: variant.stock.unwrap_or(0) as f64,
                is_active: product.active && variant.active,
            }
        })
        .collect()
}

/// Pulls the full product catalog from the cloud (`GET /api/products`,
/// paged) and upserts it into the local `products` table by barcode.
/// Manual and user-triggered only (Sincronización screen's button) - never
/// runs automatically on app start.
#[tauri::command]
pub async fn sync_products(state: State<'_, AppState>) -> Result<SyncSummary, String> {
    const LOC: &str = "sync::sync_products";
    logging::step(LOC, "iniciando sincronización de productos");

    let mut all_rows: Vec<LocalProductRow> = Vec::new();
    let mut products_fetched: i64 = 0;
    let mut page: u32 = 0;

    loop {
        let url = format!(
            "{}/api/products?sort=name,asc&page={}&size={}",
            API_BASE_URL, page, PAGE_SIZE
        );

        let response = crate::http_client::get(&url).await?;

        if !response.is_success() {
            return Err(format!(
                "El servidor respondió {} al sincronizar productos",
                response.status
            ));
        }

        let page_data: RemotePage = crate::http_client::parse_json(LOC, &response.body)?;

        products_fetched += page_data.content.len() as i64;
        let is_last = page_data.page.number + 1 >= page_data.page.total_pages;
        logging::step(
            LOC,
            format!(
                "página {} recibida: {} productos, last={}",
                page,
                page_data.content.len(),
                is_last
            ),
        );

        for product in page_data.content {
            all_rows.extend(flatten(product));
        }

        if is_last {
            break;
        }
        page += 1;
    }

    logging::step(LOC, format!("catálogo completo: {} filas a guardar en SQLite", all_rows.len()));

    let db = state.db.lock().map_err(|e| e.to_string())?;

    // Captured before the upsert loop so every row touched by this run gets
    // a `last_sync_at` at or after this instant - anything still older once
    // the loop finishes was NOT in this run's feed at all (deleted, or
    // unpublished, in the cloud) and gets deactivated below instead of left
    // to silently drift out of date.
    let run_started_at: String = db
        .conn
        .query_row("SELECT datetime('now', 'localtime')", [], |row| row.get(0))
        .map_err(|e| e.to_string())?;

    for row in &all_rows {
        db.conn
            .execute(
                "INSERT INTO products (remote_variant_id, barcode, product_name, unit_price, category, is_active, image_path, stock_quantity, last_sync_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, datetime('now', 'localtime'))
                 ON CONFLICT(barcode) DO UPDATE SET
                     remote_variant_id = excluded.remote_variant_id,
                     product_name      = excluded.product_name,
                     unit_price        = excluded.unit_price,
                     category          = excluded.category,
                     is_active         = excluded.is_active,
                     image_path        = excluded.image_path,
                     stock_quantity    = excluded.stock_quantity,
                     last_sync_at      = excluded.last_sync_at",
                rusqlite::params![
                    row.remote_variant_id,
                    row.barcode,
                    row.product_name,
                    row.unit_price,
                    row.category,
                    row.is_active as i32,
                    row.image_path,
                    row.stock_quantity,
                ],
            )
            .map_err(|e| format!("Error guardando '{}': {}", row.barcode, e))?;
    }

    // Never deletes the row (keeps sale history intact via sale_items'
    // FK) - just flips is_active off for anything no longer in the feed,
    // same visual treatment as an explicit deactivation from the cloud.
    let products_deactivated = db
        .conn
        .execute(
            "UPDATE products SET is_active = 0 WHERE last_sync_at IS NOT NULL AND last_sync_at < ?1",
            rusqlite::params![run_started_at],
        )
        .map_err(|e| e.to_string())?;

    logging::step(
        LOC,
        format!(
            "sincronización terminada: {} productos, {} variantes, {} desactivados",
            products_fetched,
            all_rows.len(),
            products_deactivated
        ),
    );

    Ok(SyncSummary {
        products_fetched,
        variants_synced: all_rows.len() as i64,
        products_deactivated: products_deactivated as i64,
    })
}
