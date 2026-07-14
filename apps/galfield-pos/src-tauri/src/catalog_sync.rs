use serde::{Deserialize, Serialize};
use tauri::State;

use crate::http_client::{self, API_BASE_URL};
use crate::logging;
use crate::AppState;

#[derive(Deserialize)]
struct RemoteCategory {
    name: String,
    #[serde(default)]
    description: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct RemotePaymentMethod {
    payment_method_id: i64,
    method_name: String,
    // The cloud sends `null` for most methods (only a few have an uploaded
    // image) — plain `String` fails to deserialize `null` at all, which is
    // exactly what was breaking the whole sync on the first method without
    // an image.
    #[serde(default)]
    image_url: Option<String>,
    active: bool,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PaymentMethodSyncSummary {
    pub synced: i64,
    pub deactivated: i64,
}

/// Pulls `GET /api/categories` and does a full replace: `categories` has no
/// local foreign key pointing at it (`products.category` is a plain
/// denormalized text column, not `REFERENCES categories(id)` — see
/// `001_initial.sql`), so nothing can break by deleting every row and
/// reinserting exactly what the cloud returned.
#[tauri::command]
pub async fn sync_categories(state: State<'_, AppState>) -> Result<i64, String> {
    const LOC: &str = "catalog_sync::sync_categories";
    logging::step(LOC, "iniciando sincronización de categorías");

    let response = http_client::get(&format!("{}/api/categories", API_BASE_URL)).await?;

    if !response.is_success() {
        return Err(format!(
            "El servidor respondió {} al sincronizar categorías",
            response.status
        ));
    }

    let categories: Vec<RemoteCategory> = http_client::parse_json(LOC, &response.body)?;
    logging::step(
        LOC,
        format!(
            "{} categorías recibidas, reemplazando tabla local",
            categories.len()
        ),
    );

    let mut db = state.db.lock().map_err(|e| e.to_string())?;
    let tx = db.conn.transaction().map_err(|e| e.to_string())?;

    tx.execute("DELETE FROM categories", [])
        .map_err(|e| e.to_string())?;

    for category in &categories {
        tx.execute(
            "INSERT INTO categories (name, description) VALUES (?1, ?2)",
            rusqlite::params![category.name, category.description],
        )
        .map_err(|e| format!("Error guardando la categoría '{}': {}", category.name, e))?;
    }

    tx.commit().map_err(|e| e.to_string())?;
    logging::step(LOC, "categorías sincronizadas");

    Ok(categories.len() as i64)
}

/// Pulls `GET /api/payment-methods`. Unlike categories, `payment_method` IS
/// referenced by a real foreign key (`sales.payment_method`) — deleting a
/// row a past sale points to would violate it (`PRAGMA foreign_keys = ON`).
/// So this never deletes: upserts by name (`payment_method.name` is
/// `UNIQUE`), then flips `is_active` off for anything the cloud stopped
/// returning — same "never delete, just deactivate" fix as `sync_products`'
/// stale sweep in `sync.rs`. `list_payment_methods` (payment_methods.rs)
/// only offers `is_active = 1` rows for new sales; existing sales keep
/// their FK regardless.
#[tauri::command]
pub async fn sync_payment_methods(
    state: State<'_, AppState>,
) -> Result<PaymentMethodSyncSummary, String> {
    const LOC: &str = "catalog_sync::sync_payment_methods";
    logging::step(LOC, "iniciando sincronización de métodos de pago");

    let response = http_client::get(&format!("{}/api/payment-methods", API_BASE_URL)).await?;

    if !response.is_success() {
        return Err(format!(
            "El servidor respondió {} al sincronizar métodos de pago",
            response.status
        ));
    }

    let methods: Vec<RemotePaymentMethod> = http_client::parse_json(LOC, &response.body)?;
    logging::step(LOC, format!("{} métodos de pago recibidos", methods.len()));

    let db = state.db.lock().map_err(|e| e.to_string())?;

    for method in &methods {
        let image_path = method.image_url.as_deref().unwrap_or("");
        db.conn
            .execute(
                "INSERT INTO payment_method (name, url, is_active, remote_payment_method_id) VALUES (?1, ?2, ?3, ?4)
                 ON CONFLICT(name) DO UPDATE SET is_active = excluded.is_active, remote_payment_method_id = excluded.remote_payment_method_id",
                rusqlite::params![method.method_name, image_path, method.active as i32, method.payment_method_id],
            )
            .map_err(|e| {
                format!(
                    "Error guardando el método de pago '{}': {}",
                    method.method_name, e
                )
            })?;
    }

    let deactivated = if methods.is_empty() {
        // An empty cloud response almost certainly means something's wrong
        // upstream, not "delete every payment method" — leave existing rows
        // alone rather than deactivating the whole table on a fluke.
        0
    } else {
        let placeholders = methods.iter().map(|_| "?").collect::<Vec<_>>().join(",");
        let sql = format!(
            "UPDATE payment_method SET is_active = 0 WHERE is_active = 1 AND name NOT IN ({})",
            placeholders
        );
        let names: Vec<&str> = methods.iter().map(|m| m.method_name.as_str()).collect();
        let params = rusqlite::params_from_iter(names.iter());
        db.conn.execute(&sql, params).map_err(|e| e.to_string())?
    };

    logging::step(
        LOC,
        format!(
            "métodos de pago sincronizados, {} desactivados",
            deactivated
        ),
    );

    Ok(PaymentMethodSyncSummary {
        synced: methods.len() as i64,
        deactivated: deactivated as i64,
    })
}
