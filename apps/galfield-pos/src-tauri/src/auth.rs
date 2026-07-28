//! Employee login for this desktop POS terminal — caches an issued JWT plus
//! an employee/role snapshot locally (`employee_session`, a single-row
//! table, migration `010_employee_session.sql`) so a cashier logs in once
//! per calendar day, not once per app launch. See backend/pos's CLAUDE.md
//! ("Employee login / JWT") for the cloud endpoint's contract, and
//! specs/01-login-empleados-roles.md for the full spec this implements
//! across all three components.
//!
//! Desktop only ever sends `terminalCode` (a Cajero-type login) — there is
//! no "admin logs into the desktop POS" flow in this spec, matching
//! `invoice_numbering.rs`'s existing `invoicing.terminal_code` setting
//! (Configuración → Reglas y Sincronización), which this reuses as-is.

use std::collections::HashMap;

use serde::{Deserialize, Serialize};
use tauri::State;

use crate::http_client;
use crate::logging;
use crate::AppState;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct LoginRequestBody {
    username: String,
    password: String,
    terminal_code: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct LoginResponseBody {
    token: String,
    employee_id: i64,
    username: String,
    role_name: String,
    permissions: HashMap<String, bool>,
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct EmployeeSession {
    pub employee_id: i64,
    pub username: String,
    pub role_name: String,
    pub permissions: HashMap<String, bool>,
    /// ISO datetime, always the next midnight in America/Bogota — see the
    /// SQL comment in `login` below for how this is computed without a
    /// timezone crate. Checking it against "now" and forcing a fresh login
    /// once it's passed is spec step 17, not implemented here yet —
    /// `get_session` returns whatever is cached as-is.
    pub expires_at: String,
}

fn configured_terminal_code(db: &crate::db::Database) -> String {
    db.conn
        .query_row(
            "SELECT value_property FROM app_settings WHERE key_property = 'invoicing.terminal_code'",
            [],
            |row| row.get(0),
        )
        .unwrap_or_default()
}

/// This terminal's cached employee JWT, for `http_client.rs` to attach as
/// `Authorization: Bearer <token>` on every authenticated call (spec step
/// 16) — `None` if nothing is cached (no session, or logged out).
pub fn auth_token(db: &crate::db::Database) -> Option<String> {
    db.conn
        .query_row("SELECT jwt FROM employee_session WHERE id = 1", [], |row| row.get(0))
        .ok()
}

#[tauri::command]
pub async fn login(
    state: State<'_, AppState>,
    username: String,
    password: String,
) -> Result<EmployeeSession, String> {
    const LOC: &str = "auth::login";

    let (api_base_url, terminal_code) = {
        let db = state.db.lock().map_err(|e| e.to_string())?;
        (http_client::api_base_url(&db), configured_terminal_code(&db))
    };

    if terminal_code.trim().is_empty() {
        return Err(
            "Configura el código de esta terminal en Configuración antes de iniciar sesión.".to_string(),
        );
    }

    logging::step(LOC, format!("intentando login de '{username}' en terminal '{terminal_code}'"));

    let response = http_client::post_json(
        &format!("{api_base_url}/api/auth/login"),
        &LoginRequestBody {
            username: username.clone(),
            password,
            terminal_code,
        },
        // Never sends a token - login is always public (see backend/pos's
        // SecurityConfig), and any previously-cached token here would be
        // for whichever employee was logged in before, not the one
        // attempting to log in now.
        None,
    )
    .await?;

    if !response.is_success() {
        logging::step(LOC, format!("login rechazado: {}", response.status));
        // Same generic message as the backend's own AuthService - never
        // reveal which specific check failed (unknown user, wrong
        // password, terminal not assigned, role not allowed here).
        return Err("Usuario, clave o terminal inválidos".to_string());
    }

    let parsed: LoginResponseBody = http_client::parse_json(LOC, &response.body)?;
    let permissions_json = serde_json::to_string(&parsed.permissions).map_err(|e| e.to_string())?;

    let db = state.db.lock().map_err(|e| e.to_string())?;
    let expires_at = write_session(
        &db,
        parsed.employee_id,
        &parsed.username,
        &parsed.role_name,
        &permissions_json,
        &parsed.token,
    )?;

    logging::step(LOC, format!("sesión iniciada para '{}', vence {}", parsed.username, expires_at));

    Ok(EmployeeSession {
        employee_id: parsed.employee_id,
        username: parsed.username,
        role_name: parsed.role_name,
        permissions: parsed.permissions,
        expires_at,
    })
}

/// Checked on every app startup and whenever the window regains focus (see
/// `useEmployeeSession.ts`'s `loadSession`/`refreshSession`) - spec step 17:
/// "cada arranque/foreground compara employee_session.expires_at contra la
/// hora local; limpia sesión y fuerza login si ya venció".
#[tauri::command]
pub fn get_session(state: State<AppState>) -> Result<Option<EmployeeSession>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;
    session_if_valid(&db)
}

/// Clears the cached session - the CHECK (id = 1) row simply gets deleted,
/// not flipped to some "logged out" flag, since there's nothing to keep
/// once a session is over (see spec's "Cerrar sesión" - lets another
/// cashier log into the same terminal the same day).
#[tauri::command]
pub fn logout(state: State<AppState>) -> Result<(), String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;
    clear_session(&db)
}

/// Upserts the single cached session row and returns the `expires_at` SQLite
/// just computed for it. Split out from `login` (a `#[tauri::command]`,
/// async, needs a live `AppState`/network) so this pure DB step can be
/// exercised directly in tests below without either of those.
fn write_session(
    db: &crate::db::Database,
    employee_id: i64,
    username: &str,
    role_name: &str,
    permissions_json: &str,
    jwt: &str,
) -> Result<String, String> {
    db.conn
        .execute(
            "INSERT INTO employee_session (id, employee_id, username, role_name, permissions, jwt, expires_at)
             VALUES (
                 1, ?1, ?2, ?3, ?4, ?5,
                 -- Next midnight in America/Bogota: SQLite's 'now' is UTC,
                 -- shift -5h (Colombia has no DST, so this is exact) to get
                 -- Bogota wall-clock, then jump to the start of the next day.
                 datetime(datetime('now', '-5 hours'), 'start of day', '+1 day')
             )
             ON CONFLICT(id) DO UPDATE SET
                 employee_id = excluded.employee_id,
                 username    = excluded.username,
                 role_name   = excluded.role_name,
                 permissions = excluded.permissions,
                 jwt         = excluded.jwt,
                 expires_at  = excluded.expires_at",
            rusqlite::params![employee_id, username, role_name, permissions_json, jwt],
        )
        .map_err(|e| e.to_string())?;

    db.conn
        .query_row("SELECT expires_at FROM employee_session WHERE id = 1", [], |row| row.get(0))
        .map_err(|e| e.to_string())
}

fn read_session(db: &crate::db::Database) -> Result<Option<EmployeeSession>, String> {
    db.conn
        .query_row(
            "SELECT employee_id, username, role_name, permissions, expires_at FROM employee_session WHERE id = 1",
            [],
            |row| {
                let permissions_json: String = row.get(3)?;
                let permissions: HashMap<String, bool> = serde_json::from_str(&permissions_json)
                    .unwrap_or_default();
                Ok(EmployeeSession {
                    employee_id: row.get(0)?,
                    username: row.get(1)?,
                    role_name: row.get(2)?,
                    permissions,
                    expires_at: row.get(4)?,
                })
            },
        )
        .map(Some)
        .or_else(|e| match e {
            rusqlite::Error::QueryReturnedNoRows => Ok(None),
            e => Err(e.to_string()),
        })
}

fn clear_session(db: &crate::db::Database) -> Result<(), String> {
    db.conn
        .execute("DELETE FROM employee_session WHERE id = 1", [])
        .map_err(|e| e.to_string())?;
    Ok(())
}

/// `read_session`, but enforcing `expires_at` — returns `None` (clearing the
/// stale row along the way) once the cached session's `expires_at` has
/// passed, instead of returning it as if it were still valid. The
/// comparison happens in SQL (`expires_at <= datetime('now', '-5 hours')`,
/// same Bogota-shift as `write_session` computes `expires_at` with) rather
/// than parsing the stored datetime string in Rust — consistent with the
/// rest of this codebase never doing date arithmetic outside SQLite.
fn session_if_valid(db: &crate::db::Database) -> Result<Option<EmployeeSession>, String> {
    let Some(session) = read_session(db)? else {
        return Ok(None);
    };

    let is_expired: bool = db
        .conn
        .query_row(
            "SELECT ?1 <= datetime('now', '-5 hours')",
            rusqlite::params![session.expires_at],
            |row| row.get(0),
        )
        .map_err(|e| e.to_string())?;

    if is_expired {
        clear_session(db)?;
        return Ok(None);
    }

    Ok(Some(session))
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::db::Database;
    use std::path::PathBuf;

    /// A fresh on-disk SQLite DB (all migrations applied, including
    /// `010_employee_session.sql`) per test, in its own temp dir so
    /// parallel tests never collide.
    fn temp_db() -> (Database, PathBuf) {
        let dir = std::env::temp_dir().join(format!("galfield-pos-auth-test-{}", uuid::Uuid::new_v4()));
        let db = Database::init(dir.clone()).expect("failed to init temp database");
        (db, dir)
    }

    fn cleanup(dir: PathBuf) {
        let _ = std::fs::remove_dir_all(dir);
    }

    #[test]
    fn write_session_persists_the_expected_row() {
        let (db, dir) = temp_db();
        let permissions_json = r#"{"pos":true,"inventario":false,"reportes":false,"sync":true}"#;

        let expires_at = write_session(&db, 7, "jperez", "Cajero", permissions_json, "jwt-token-1")
            .expect("write_session should succeed");
        assert!(!expires_at.is_empty());

        let session = read_session(&db)
            .expect("read_session should succeed")
            .expect("a session should now be cached");
        assert_eq!(session.employee_id, 7);
        assert_eq!(session.username, "jperez");
        assert_eq!(session.role_name, "Cajero");
        assert_eq!(session.permissions.get("pos"), Some(&true));
        assert_eq!(session.permissions.get("inventario"), Some(&false));
        assert_eq!(session.expires_at, expires_at);

        // expires_at must be in the future (next Bogota midnight), not "now"
        // or something in the past - catches a regression to a formula that
        // drops the '+1 day' jump.
        let is_future: bool = db
            .conn
            .query_row(
                "SELECT expires_at > datetime('now', '-5 hours') FROM employee_session WHERE id = 1",
                [],
                |row| row.get(0),
            )
            .unwrap();
        assert!(is_future, "expires_at ({expires_at}) should be later than the current Bogota time");

        cleanup(dir);
    }

    #[test]
    fn write_session_upserts_the_single_row_instead_of_accumulating() {
        let (db, dir) = temp_db();
        let permissions_json = "{}";

        write_session(&db, 7, "jperez", "Cajero", permissions_json, "jwt-token-1").unwrap();
        write_session(&db, 9, "amartinez", "Administrador", permissions_json, "jwt-token-2").unwrap();

        // The CHECK (id = 1) constraint would reject a second row outright
        // if this ever regressed from ON CONFLICT DO UPDATE to a plain
        // INSERT - this asserts the intended behavior directly either way.
        let count: i64 = db.conn.query_row("SELECT COUNT(*) FROM employee_session", [], |row| row.get(0)).unwrap();
        assert_eq!(count, 1);

        let session = read_session(&db).unwrap().unwrap();
        assert_eq!(session.employee_id, 9);
        assert_eq!(session.username, "amartinez");

        cleanup(dir);
    }

    #[test]
    fn read_session_returns_none_when_nothing_is_cached() {
        let (db, dir) = temp_db();
        assert!(read_session(&db).unwrap().is_none());
        cleanup(dir);
    }

    #[test]
    fn clear_session_removes_the_row() {
        let (db, dir) = temp_db();
        write_session(&db, 1, "cajero1", "Cajero", "{}", "jwt").unwrap();
        assert!(read_session(&db).unwrap().is_some());

        clear_session(&db).unwrap();
        assert!(read_session(&db).unwrap().is_none());

        cleanup(dir);
    }

    #[test]
    fn auth_token_reflects_whatever_is_currently_cached() {
        let (db, dir) = temp_db();
        assert_eq!(auth_token(&db), None, "no session cached yet");

        write_session(&db, 1, "cajero1", "Cajero", "{}", "jwt-first").unwrap();
        assert_eq!(auth_token(&db), Some("jwt-first".to_string()));

        // Logging in again (e.g. a different cashier on the same terminal)
        // must update the token http_client.rs reads, not just the
        // employee/role fields - this is what login() relies on to keep
        // http_client.rs's Authorization header in sync after a session change.
        write_session(&db, 2, "cajero2", "Cajero", "{}", "jwt-second").unwrap();
        assert_eq!(auth_token(&db), Some("jwt-second".to_string()));

        clear_session(&db).unwrap();
        assert_eq!(auth_token(&db), None, "logout should clear the cached token too");

        cleanup(dir);
    }

    #[test]
    fn session_if_valid_returns_the_session_before_expiry() {
        let (db, dir) = temp_db();
        write_session(&db, 7, "jperez", "Cajero", "{}", "jwt").unwrap();

        let session = session_if_valid(&db).unwrap();
        assert!(session.is_some(), "a freshly-written session (next Bogota midnight) should still be valid");
        // Not cleared - still readable directly too.
        assert!(read_session(&db).unwrap().is_some());

        cleanup(dir);
    }

    #[test]
    fn session_if_valid_clears_and_returns_none_once_expired() {
        let (db, dir) = temp_db();
        write_session(&db, 7, "jperez", "Cajero", "{}", "jwt").unwrap();

        // Backdate expires_at directly (write_session always computes a
        // future one) to simulate a session left over from a previous day -
        // this is exactly the "adelantar expires_at a mano en la DB local"
        // manual test step from the spec, just automated.
        db.conn
            .execute("UPDATE employee_session SET expires_at = '2000-01-01 00:00:00' WHERE id = 1", [])
            .unwrap();

        let session = session_if_valid(&db).unwrap();
        assert!(session.is_none(), "an expired session must not be returned as valid");

        // The stale row must actually be gone, not just skipped this once -
        // a later direct read (e.g. from a different command in the same
        // run) must see "no session" too, not the same stale one again.
        assert!(read_session(&db).unwrap().is_none(), "expired session should be cleared, not just ignored");

        cleanup(dir);
    }
}
