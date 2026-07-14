//! Every outbound call to the cloud (`sync.rs`, `catalog_sync.rs`,
//! `sales_sync.rs`) goes through here instead of a bare `reqwest::Client`, so
//! there's exactly one place that logs what actually went over the wire —
//! method, URL, request body, response status, and response body — to
//! stdout, visible in the `npm run tauri dev` terminal. Debug visibility
//! only, not user-facing UI.
//!
//! `parse_json` is the other half of that centralization: every "the cloud
//! answered but the shape wasn't what we expected" error (a serde failure)
//! goes through here too, tagged with the caller's `module::function`, so
//! the error string the frontend shows says exactly where to look instead
//! of a bare serde message with no file/line context.

use std::sync::OnceLock;

use reqwest::StatusCode;
use serde::Serialize;

use crate::db::Database;
use crate::logging;

/// Only used if `sync.api_base_url` is somehow missing from `app_settings`
/// (a very old local DB from before migration `007_api_base_url_setting`
/// that hasn't run yet) — see `api_base_url` below for the real source.
pub const DEFAULT_API_BASE_URL: &str = "https://galfields.kinforgeworks.com";

static CLIENT: OnceLock<reqwest::Client> = OnceLock::new();

fn client() -> &'static reqwest::Client {
    CLIENT.get_or_init(reqwest::Client::new)
}

/// Reads the cloud API base URL from `app_settings` (`sync.api_base_url`,
/// editable from Configuración → Reglas y Sincronización) instead of a
/// hardcoded constant, so one installation can point at a different backend
/// without a rebuild. Callers re-read this on every sync run instead of
/// caching it — same reasoning `sales_sync.rs`'s background loop re-reads
/// `sync.sales_retry_minutes` every wake — so changing it in Configuración
/// takes effect on the next sync, not just after restarting the app.
pub fn api_base_url(db: &Database) -> String {
    db.conn
        .query_row(
            "SELECT value_property FROM app_settings WHERE key_property = 'sync.api_base_url'",
            [],
            |row| row.get::<_, String>(0),
        )
        .unwrap_or_else(|_| DEFAULT_API_BASE_URL.to_string())
}

pub struct HttpResponse {
    pub status: StatusCode,
    pub body: String,
}

impl HttpResponse {
    pub fn is_success(&self) -> bool {
        self.status.is_success()
    }
}

pub async fn get(url: &str) -> Result<HttpResponse, String> {
    logging::step("http_client::get", format!("--> GET {url}"));
    send(client().get(url)).await
}

pub async fn post_json<T: Serialize>(url: &str, body: &T) -> Result<HttpResponse, String> {
    let body_json = serde_json::to_string(body).unwrap_or_else(|_| "<unserializable body>".to_string());
    logging::step("http_client::post_json", format!("--> POST {url} | body: {body_json}"));
    send(client().post(url).json(body)).await
}

async fn send(request: reqwest::RequestBuilder) -> Result<HttpResponse, String> {
    let response = request.send().await.map_err(|e| {
        logging::step("http_client::send", format!("<-- connection error: {e}"));
        format!("No se pudo conectar con el servidor: {e}")
    })?;

    let status = response.status();
    let body = response.text().await.unwrap_or_default();
    logging::step("http_client::send", format!("<-- {status} | body: {body}"));

    Ok(HttpResponse { status, body })
}

/// Deserializes a cloud response body, tagging any failure with `context`
/// (pass `"module::function"`, e.g. `"sync::sync_products"`) so the error
/// that reaches the frontend names the exact call site to open — instead of
/// three copy-pasted `.map_err(|e| format!("Respuesta inesperada..."))`
/// sites each saying the same generic thing with no way to trace it back.
pub fn parse_json<T: serde::de::DeserializeOwned>(context: &str, body: &str) -> Result<T, String> {
    serde_json::from_str(body).map_err(|e| {
        logging::step(context, format!("ERROR parseando respuesta del servidor: {e}"));
        format!("Respuesta inesperada del servidor en {context}: {e}")
    })
}
