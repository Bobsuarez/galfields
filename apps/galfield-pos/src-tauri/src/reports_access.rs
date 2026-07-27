//! Validates the 6-digit access code a cashier types before entering the
//! Reportes module, against the code most recently generated from
//! apps/galfields-mobile's Configuración → Acceso a Reportes (see
//! backend/pos's CLAUDE.md, "Reports access code"). This is a plain
//! validate-and-return-a-bool call, not a sync step — nothing is written
//! to the local SQLite DB, since the gate is re-checked on every entry to
//! Reportes rather than cached locally.

use serde::{Deserialize, Serialize};
use tauri::State;

use crate::http_client;
use crate::logging;
use crate::AppState;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ValidateRequestBody {
    code: String,
}

#[derive(Deserialize)]
struct ValidateResponse {
    valid: bool,
}

#[tauri::command]
pub async fn validate_reports_access_code(
    state: State<'_, AppState>,
    code: String,
) -> Result<bool, String> {
    const LOC: &str = "reports_access::validate_reports_access_code";

    // Scoped so the MutexGuard drops before the `.await` below.
    let api_base_url = {
        let db = state.db.lock().map_err(|e| e.to_string())?;
        http_client::api_base_url(&db)
    };

    logging::step(LOC, "validando código de acceso a reportes");

    let response = http_client::post_json(
        &format!("{api_base_url}/api/reports-access-code/validate"),
        &ValidateRequestBody { code },
    )
    .await?;

    if !response.is_success() {
        return Err(format!(
            "El servidor respondió {} al validar el código de acceso",
            response.status
        ));
    }

    let parsed: ValidateResponse = http_client::parse_json(LOC, &response.body)?;
    Ok(parsed.valid)
}
