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

use crate::logging;

pub const API_BASE_URL: &str = "https://galfields.kinforgeworks.com";

static CLIENT: OnceLock<reqwest::Client> = OnceLock::new();

fn client() -> &'static reqwest::Client {
    CLIENT.get_or_init(reqwest::Client::new)
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
