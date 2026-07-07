use std::collections::HashMap;
use tauri::State;

use crate::AppState;

#[tauri::command]
pub fn get_settings(state: State<AppState>) -> Result<HashMap<String, String>, String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    let mut stmt = db
        .conn
        .prepare("SELECT key_property, value_property FROM app_settings")
        .map_err(|e| e.to_string())?;

    let mut map = HashMap::new();
    let rows = stmt
        .query_map([], |row| Ok((row.get::<_, String>(0)?, row.get::<_, String>(1)?)))
        .map_err(|e| e.to_string())?;

    for row in rows {
        let (k, v) = row.map_err(|e| e.to_string())?;
        map.insert(k, v);
    }

    Ok(map)
}

#[tauri::command]
pub fn save_settings(
    state: State<AppState>,
    settings: HashMap<String, String>,
) -> Result<(), String> {
    let db = state.db.lock().map_err(|e| e.to_string())?;

    for (key, value) in &settings {
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
    }

    Ok(())
}
