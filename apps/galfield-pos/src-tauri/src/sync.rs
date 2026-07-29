use rusqlite::{Connection, OptionalExtension};
use serde::{Deserialize, Serialize};
use tauri::State;

use crate::logging;
use crate::AppState;

const PAGE_SIZE: u32 = 100;

#[derive(Deserialize)]
struct RemoteAttribute {
    value: String,
}

/// A sale unit for a variant (e.g. "Media"/"Completa" cajetilla of the same
/// SKU, sharing its stock) - see backend/pos's CLAUDE.md, "Sale units with
/// conversion factors". `stock` already arrives converted to "how many of
/// this presentation are available" (`floorDiv(variant stock, factor)`) -
/// this module does no math on it, same as `RemoteVariant.stock` already
/// worked before this feature existed.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct RemoteProductUnit {
    product_unit_id: i64,
    unit_name: String,
    conversion_factor: i64,
    unit_price: f64,
    stock: i64,
    #[serde(default)]
    barcode: Option<String>,
    active: bool,
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
    #[serde(default)]
    units: Vec<RemoteProductUnit>,
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

/// One local `products` row per cloud sale unit (spec
/// `03-unidades-venta-conversion`) - or, for a variant with none configured
/// yet (see backend/pos's CLAUDE.md "Known gap" note), one row per cloud
/// *variant*, exactly like before this feature existed. The local schema is
/// deliberately flat (see 001_initial.sql) - a single barcode/price/stock
/// per row - and checkout scans against a specific row's own barcode, so
/// every sellable presentation needs its own row to be scannable.
///
/// `remote_variant_id` is what groups sibling rows sharing the same
/// physical stock (e.g. "Media"/"Completa" of the same SKU both point at
/// the same variant) - no separate grouping column needed, since
/// backend/pos's `product_units` hangs off `product_variants.variant_id`,
/// not `products.product_id`. `remote_product_unit_id` identifies this
/// exact row's own cloud unit (`None` for the no-units-yet fallback rows).
struct LocalProductRow {
    remote_variant_id: i64,
    remote_product_unit_id: Option<i64>,
    barcode: Option<String>,
    product_name: String,
    unit_price: f64,
    unit_name: String,
    conversion_factor: i64,
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
        .flat_map(|variant| {
            // Multi-variant products need the variant called out in the
            // name (e.g. "Camiseta Basica - Rojo M") since the local table
            // has no separate variant/attribute concept to disambiguate them.
            let variant_name = if multi_variant {
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

            let category = product.category_name.clone().unwrap_or_default();
            let image_path = variant
                .image_url
                .clone()
                .or_else(|| product.image_url.clone())
                .unwrap_or_default();
            let is_active_base = product.active && variant.active;

            if variant.units.is_empty() {
                // No sale units configured for this variant yet (a variant
                // created after V10__product_units.sql, before anyone added
                // one - see backend/pos's CLAUDE.md "Known gap" note) - fall
                // back to exactly the pre-this-feature behavior: one row,
                // sourced straight from the variant itself. Skipped
                // entirely if it has no barcode either, same filter this
                // function always applied before sale units existed.
                if variant.barcode.trim().is_empty() {
                    return Vec::new();
                }

                vec![LocalProductRow {
                    remote_variant_id: variant.variant_id,
                    remote_product_unit_id: None,
                    barcode: Some(variant.barcode),
                    product_name: variant_name,
                    unit_price: variant.price,
                    unit_name: "Unidad".to_string(),
                    conversion_factor: 1,
                    category,
                    image_path,
                    stock_quantity: variant.stock.unwrap_or(0) as f64,
                    is_active: is_active_base,
                }]
            } else {
                // Multiple sale units on the same variant need their own
                // name suffix too (e.g. "Cigarrillos Malboro - Completa"),
                // same reasoning as the multi-variant attribute suffix
                // above - a variant with a single unit keeps its plain name,
                // no regression for the common (not-yet-using-units) case.
                let multi_unit = variant.units.len() > 1;

                variant
                    .units
                    .into_iter()
                    .map(|unit| {
                        let product_name = if multi_unit {
                            format!("{} - {}", variant_name, unit.unit_name)
                        } else {
                            variant_name.clone()
                        };

                        LocalProductRow {
                            remote_variant_id: variant.variant_id,
                            remote_product_unit_id: Some(unit.product_unit_id),
                            barcode: unit.barcode,
                            product_name,
                            unit_price: unit.unit_price,
                            unit_name: unit.unit_name,
                            conversion_factor: unit.conversion_factor,
                            category: category.clone(),
                            image_path: image_path.clone(),
                            stock_quantity: unit.stock as f64,
                            is_active: is_active_base && unit.active,
                        }
                    })
                    .collect()
            }
        })
        .collect()
}

/// Finds the local row this remote row should update, if any. Prefers a
/// match on `remote_product_unit_id` (this row's real cloud identity going
/// forward); falls back to `barcode` for the one-time transition case - a
/// row synced before migration `011_product_units` only has a barcode to
/// match on, since `remote_product_unit_id` didn't exist yet (and would
/// otherwise collide with the incoming row on `barcode`'s UNIQUE
/// constraint instead of updating in place). The no-units fallback path
/// (`remote_product_unit_id: None`) always matches by barcode, exactly
/// like every sync before this feature existed.
fn find_existing_row_id(conn: &Connection, row: &LocalProductRow) -> rusqlite::Result<Option<i64>> {
    if let Some(unit_id) = row.remote_product_unit_id {
        let by_unit: Option<i64> = conn
            .query_row(
                "SELECT id FROM products WHERE remote_product_unit_id = ?1",
                rusqlite::params![unit_id],
                |r| r.get(0),
            )
            .optional()?;
        if by_unit.is_some() {
            return Ok(by_unit);
        }
    }

    match &row.barcode {
        Some(barcode) if !barcode.is_empty() => conn
            .query_row(
                "SELECT id FROM products WHERE barcode = ?1",
                rusqlite::params![barcode],
                |r| r.get(0),
            )
            .optional(),
        _ => Ok(None),
    }
}

/// Pulls the full product catalog from the cloud (`GET /api/products`,
/// paged) and upserts it into the local `products` table (matched by
/// `remote_product_unit_id`, falling back to `barcode` — see
/// `find_existing_row_id`). Manual and user-triggered only (Sincronización
/// screen's button) - never runs automatically on app start.
#[tauri::command]
pub async fn sync_products(state: State<'_, AppState>) -> Result<SyncSummary, String> {
    const LOC: &str = "sync::sync_products";
    logging::step(LOC, "iniciando sincronización de productos");

    // Scoped so the MutexGuard drops before the loop below, which awaits.
    let (api_base_url, token) = {
        let db = state.db.lock().map_err(|e| e.to_string())?;
        (crate::http_client::api_base_url(&db), crate::auth::auth_token(&db))
    };

    let mut all_rows: Vec<LocalProductRow> = Vec::new();
    let mut products_fetched: i64 = 0;
    let mut page: u32 = 0;

    loop {
        let url = format!(
            "{}/api/products?sort=name,asc&page={}&size={}",
            api_base_url, page, PAGE_SIZE
        );

        let response = crate::http_client::get(&url, token.as_deref()).await?;

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
        let existing_id = find_existing_row_id(&db.conn, row).map_err(|e| e.to_string())?;

        if let Some(id) = existing_id {
            db.conn
                .execute(
                    "UPDATE products SET
                         remote_variant_id      = ?1,
                         remote_product_unit_id = ?2,
                         barcode                = ?3,
                         product_name           = ?4,
                         unit_price             = ?5,
                         unit_name               = ?6,
                         conversion_factor       = ?7,
                         category                = ?8,
                         is_active               = ?9,
                         image_path              = ?10,
                         stock_quantity          = ?11,
                         last_sync_at            = datetime('now', 'localtime')
                     WHERE id = ?12",
                    rusqlite::params![
                        row.remote_variant_id,
                        row.remote_product_unit_id,
                        row.barcode,
                        row.product_name,
                        row.unit_price,
                        row.unit_name,
                        row.conversion_factor,
                        row.category,
                        row.is_active as i32,
                        row.image_path,
                        row.stock_quantity,
                        id,
                    ],
                )
                .map_err(|e| format!("Error actualizando '{}': {}", row.product_name, e))?;
        } else {
            db.conn
                .execute(
                    "INSERT INTO products (
                         remote_variant_id, remote_product_unit_id, barcode, product_name,
                         unit_price, unit_name, conversion_factor, category, is_active,
                         image_path, stock_quantity, last_sync_at
                     ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, datetime('now', 'localtime'))",
                    rusqlite::params![
                        row.remote_variant_id,
                        row.remote_product_unit_id,
                        row.barcode,
                        row.product_name,
                        row.unit_price,
                        row.unit_name,
                        row.conversion_factor,
                        row.category,
                        row.is_active as i32,
                        row.image_path,
                        row.stock_quantity,
                    ],
                )
                .map_err(|e| format!("Error guardando '{}': {}", row.product_name, e))?;
        }
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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::db::Database;
    use std::path::PathBuf;

    fn temp_db() -> (Database, PathBuf) {
        let dir = std::env::temp_dir().join(format!("galfield-pos-sync-test-{}", uuid::Uuid::new_v4()));
        let db = Database::init(dir.clone()).expect("failed to init temp database");
        (db, dir)
    }

    fn cleanup(dir: PathBuf) {
        let _ = std::fs::remove_dir_all(dir);
    }

    fn sample_variant(units: Vec<RemoteProductUnit>) -> RemoteVariant {
        RemoteVariant {
            variant_id: 42,
            barcode: "7701234500001".to_string(),
            price: 5000.0,
            stock: Some(980),
            image_url: None,
            active: true,
            attributes: Vec::new(),
            units,
        }
    }

    #[test]
    fn flatten_variant_with_no_units_falls_back_to_old_row_shape() {
        let product = RemoteProduct {
            name: "Cigarrillos Malboro".to_string(),
            category_name: Some("Cigarrería".to_string()),
            image_url: None,
            active: true,
            variants: vec![sample_variant(Vec::new())],
        };

        let rows = flatten(product);

        assert_eq!(rows.len(), 1);
        let row = &rows[0];
        assert_eq!(row.remote_variant_id, 42);
        assert_eq!(row.remote_product_unit_id, None);
        assert_eq!(row.barcode.as_deref(), Some("7701234500001"));
        assert_eq!(row.product_name, "Cigarrillos Malboro");
        assert_eq!(row.unit_name, "Unidad");
        assert_eq!(row.conversion_factor, 1);
        assert_eq!(row.stock_quantity, 980.0);
    }

    #[test]
    fn flatten_variant_with_multiple_units_produces_one_row_per_unit_sharing_variant_id() {
        let units = vec![
            RemoteProductUnit {
                product_unit_id: 100,
                unit_name: "Unidad".to_string(),
                conversion_factor: 1,
                unit_price: 250.0,
                stock: 980,
                barcode: Some("7701234500001".to_string()),
                active: true,
            },
            RemoteProductUnit {
                product_unit_id: 101,
                unit_name: "Completa".to_string(),
                conversion_factor: 20,
                unit_price: 4500.0,
                stock: 49,
                barcode: None,
                active: true,
            },
        ];
        let product = RemoteProduct {
            name: "Cigarrillos Malboro".to_string(),
            category_name: Some("Cigarrería".to_string()),
            image_url: None,
            active: true,
            variants: vec![sample_variant(units)],
        };

        let rows = flatten(product);

        assert_eq!(rows.len(), 2);
        assert_eq!(rows[0].remote_variant_id, 42);
        assert_eq!(rows[1].remote_variant_id, 42);
        assert_eq!(rows[0].remote_product_unit_id, Some(100));
        assert_eq!(rows[1].remote_product_unit_id, Some(101));
        assert_eq!(rows[0].product_name, "Cigarrillos Malboro - Unidad");
        assert_eq!(rows[1].product_name, "Cigarrillos Malboro - Completa");
        assert_eq!(rows[1].barcode, None);
        assert_eq!(rows[1].conversion_factor, 20);
        assert_eq!(rows[1].stock_quantity, 49.0);
    }

    #[test]
    fn flatten_inactive_unit_is_kept_but_marked_inactive_not_skipped() {
        let units = vec![RemoteProductUnit {
            product_unit_id: 200,
            unit_name: "Media".to_string(),
            conversion_factor: 10,
            unit_price: 2500.0,
            stock: 5,
            barcode: None,
            active: false,
        }];
        let product = RemoteProduct {
            name: "Test".to_string(),
            category_name: None,
            image_url: None,
            active: true,
            variants: vec![sample_variant(units)],
        };

        let rows = flatten(product);

        assert_eq!(rows.len(), 1);
        assert!(!rows[0].is_active);
    }

    /// The transition case: a row synced before migration `011_product_units`
    /// only has a barcode to match on (`remote_product_unit_id` didn't exist
    /// yet). The first sync after the migration must UPDATE that same row
    /// instead of inserting a duplicate that then collides with it on
    /// `barcode`'s UNIQUE constraint.
    #[test]
    fn find_existing_row_id_matches_pre_migration_row_by_barcode() {
        let (db, dir) = temp_db();

        db.conn
            .execute(
                "INSERT INTO products (barcode, product_name, unit_price, stock_quantity) VALUES (?1, ?2, ?3, ?4)",
                rusqlite::params!["7701234500001", "Cigarrillos Malboro", 5000.0, 980.0],
            )
            .expect("insert pre-migration row");
        let pre_migration_id: i64 = db
            .conn
            .query_row("SELECT id FROM products WHERE barcode = ?1", ["7701234500001"], |r| r.get(0))
            .expect("row should exist");

        let incoming_row = LocalProductRow {
            remote_variant_id: 42,
            remote_product_unit_id: Some(100),
            barcode: Some("7701234500001".to_string()),
            product_name: "Cigarrillos Malboro - Unidad".to_string(),
            unit_price: 5000.0,
            unit_name: "Unidad".to_string(),
            conversion_factor: 1,
            category: String::new(),
            image_path: String::new(),
            stock_quantity: 980.0,
            is_active: true,
        };

        let matched = find_existing_row_id(&db.conn, &incoming_row).expect("query should succeed");
        assert_eq!(matched, Some(pre_migration_id));

        cleanup(dir);
    }

    /// Once a row has already synced with sale units at least once, later
    /// syncs must keep matching it by `remote_product_unit_id` even if its
    /// barcode changes (e.g. an admin edits it) or is absent.
    #[test]
    fn find_existing_row_id_matches_by_remote_product_unit_id_after_first_sync() {
        let (db, dir) = temp_db();

        db.conn
            .execute(
                "INSERT INTO products (remote_product_unit_id, barcode, product_name, unit_price, stock_quantity)
                 VALUES (?1, ?2, ?3, ?4, ?5)",
                rusqlite::params![101, "old-barcode", "Completa", 4500.0, 49.0],
            )
            .expect("insert already-synced row");
        let existing_id: i64 = db
            .conn
            .query_row("SELECT id FROM products WHERE remote_product_unit_id = ?1", [101], |r| r.get(0))
            .expect("row should exist");

        // Barcode changed on the cloud side (or now absent) - must still
        // match by remote_product_unit_id, not fail/duplicate.
        let incoming_row = LocalProductRow {
            remote_variant_id: 42,
            remote_product_unit_id: Some(101),
            barcode: None,
            product_name: "Cigarrillos Malboro - Completa".to_string(),
            unit_price: 4600.0,
            unit_name: "Completa".to_string(),
            conversion_factor: 20,
            category: String::new(),
            image_path: String::new(),
            stock_quantity: 47.0,
            is_active: true,
        };

        let matched = find_existing_row_id(&db.conn, &incoming_row).expect("query should succeed");
        assert_eq!(matched, Some(existing_id));

        cleanup(dir);
    }
}
