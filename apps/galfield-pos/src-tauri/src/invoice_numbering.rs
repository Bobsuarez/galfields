//! Pulls this terminal's DIAN-authorized invoice numbering range (prefix +
//! [range_start, range_end]) from the cloud — assigned centrally from
//! apps/galfields-mobile's Configuración → Numeración de facturas, one range
//! per terminal (see backend/pos's CLAUDE.md, "Invoice numbering ranges").
//! Same fetch-then-upsert shape as `catalog_sync::sync_categories`, but:
//!
//! - Identified by `invoicing.terminal_code`, a plain string the terminal
//!   owner types once in Configuración — not a real device/login identity
//!   (none exists yet anywhere in this codebase).
//! - Never a hard failure just because nothing is configured yet (missing
//!   `terminal_code`, or the cloud has no range for it) — those return
//!   `configured: false` instead of `Err`, so this step doesn't block the
//!   rest of the "Sincronizar catálogo" chain for a terminal that hasn't
//!   set up numbering yet. Real HTTP/parse failures still are `Err`, same
//!   as every other sync step.
//! - Only resets the local `invoicing.next_number` counter when the range
//!   actually changed (first time, or the cloud assigned a new one) —
//!   re-syncing the *same* range must never rewind a counter that's already
//!   advanced past `range_start`, or already-issued numbers would repeat.

use serde::{Deserialize, Serialize};
use tauri::State;

use crate::http_client;
use crate::logging;
use crate::AppState;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct RemoteInvoiceNumberingRange {
    prefix: String,
    range_start: i64,
    range_end: i64,
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct InvoiceNumberingSyncResult {
    pub configured: bool,
    pub message: String,
}

#[tauri::command]
pub async fn sync_invoice_numbering(
    state: State<'_, AppState>,
) -> Result<InvoiceNumberingSyncResult, String> {
    const LOC: &str = "invoice_numbering::sync_invoice_numbering";

    let (api_base_url, terminal_code) = {
        let db = state.db.lock().map_err(|e| e.to_string())?;
        let terminal_code: String = db
            .conn
            .query_row(
                "SELECT value_property FROM app_settings WHERE key_property = 'invoicing.terminal_code'",
                [],
                |row| row.get(0),
            )
            .unwrap_or_default();
        (http_client::api_base_url(&db), terminal_code)
    };

    if terminal_code.trim().is_empty() {
        logging::step(LOC, "sin código de terminal configurado, se omite");
        return Ok(InvoiceNumberingSyncResult {
            configured: false,
            message: "Configura el código de esta terminal antes de sincronizar la numeración de facturas.".to_string(),
        });
    }

    logging::step(LOC, format!("consultando rango asignado a '{terminal_code}'"));

    let response = http_client::get(&format!(
        "{}/api/invoice-numbering-ranges/by-terminal/{}",
        api_base_url, terminal_code
    ))
    .await?;

    if response.status == reqwest::StatusCode::NOT_FOUND {
        logging::step(LOC, format!("sin rango asignado a '{terminal_code}' en la nube"));
        return Ok(InvoiceNumberingSyncResult {
            configured: false,
            message: format!(
                "No hay rango de facturación asignado para '{terminal_code}' en la nube — créalo desde la app móvil."
            ),
        });
    }

    if !response.is_success() {
        return Err(format!(
            "El servidor respondió {} al sincronizar la numeración de facturas",
            response.status
        ));
    }

    let range: RemoteInvoiceNumberingRange = http_client::parse_json(LOC, &response.body)?;

    let db = state.db.lock().map_err(|e| e.to_string())?;

    let previous: Option<(String, i64, i64)> = db
        .conn
        .query_row(
            "SELECT
                (SELECT value_property FROM app_settings WHERE key_property = 'invoicing.prefix'),
                (SELECT value_property FROM app_settings WHERE key_property = 'invoicing.range_start'),
                (SELECT value_property FROM app_settings WHERE key_property = 'invoicing.range_end')",
            [],
            |row| {
                Ok((
                    row.get::<_, Option<String>>(0)?,
                    row.get::<_, Option<String>>(1)?,
                    row.get::<_, Option<String>>(2)?,
                ))
            },
        )
        .ok()
        .and_then(|(p, s, e)| Some((p?, s?.parse().ok()?, e?.parse().ok()?)));

    // Only `prefix`/`range_start` identify a genuinely new assignment.
    // `range_end` deliberately excluded: extending an existing range's end
    // (e.g. from Configuración → Numeración de facturas, to give a terminal
    // more room before it runs out) must NOT be treated as "changed", or
    // `next_number` would rewind to `range_start` and reissue invoice
    // numbers that were already printed under the old, narrower range.
    let range_changed = previous.map(|(p, s, _)| (p, s)) != Some((range.prefix.clone(), range.range_start));

    let upsert = |key: &str, value: String| -> Result<(), String> {
        db.conn
            .execute(
                "INSERT INTO app_settings (key_property, value_property)
                 VALUES (?1, ?2)
                 ON CONFLICT(key_property) DO UPDATE SET
                     value_property = excluded.value_property,
                     updated_at     = datetime('now', 'localtime')",
                rusqlite::params![key, value],
            )
            .map_err(|e| e.to_string())?;
        Ok(())
    };

    upsert("invoicing.prefix", range.prefix.clone())?;
    upsert("invoicing.range_start", range.range_start.to_string())?;
    upsert("invoicing.range_end", range.range_end.to_string())?;
    if range_changed {
        logging::step(LOC, format!("rango nuevo/cambiado, reiniciando next_number a {}", range.range_start));
        upsert("invoicing.next_number", range.range_start.to_string())?;
    }

    logging::step(LOC, format!("numeración sincronizada: {} {}-{}", range.prefix, range.range_start, range.range_end));

    Ok(InvoiceNumberingSyncResult {
        configured: true,
        message: format!("Rango asignado: {}{:06}–{}{:06}", range.prefix, range.range_start, range.prefix, range.range_end),
    })
}
