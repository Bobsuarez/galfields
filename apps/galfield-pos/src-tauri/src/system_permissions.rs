//! System-level serial port permissions — a one-shot action, not tied to any
//! single device (barcode/printer/cash-drawer/etc. all share the same OS-level
//! access requirement). Dispatched with `trigger_apply_port_permissions` and
//! reported via `peripheral-permissions-status`/`peripheral-permissions-error`
//! events, same fire-and-listen convention as the other `trigger_*` commands
//! (see "Peripheral event model" in CLAUDE.md).
//!
//! On Linux, reading/writing `/dev/ttyUSB*`/`/dev/ttyACM*` requires the OS
//! user to belong to the `dialout` group — the installer is supposed to run
//! `usermod -aG dialout <user>` but sometimes skips it. This lets the user
//! trigger that same command from inside the app instead, elevated via
//! `pkexec` (the GUI polkit prompt) since a GUI app has no TTY for `sudo`.
//! Windows serial ports don't have an equivalent OS permission gate — a port
//! that doesn't show up there is almost always a missing USB-serial driver
//! (CH340/FTDI/PL2303), which this command can't install, so it just reports
//! that instead of running anything.

use tauri::{AppHandle, Emitter};

use crate::logging;

#[derive(serde::Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsResultPayload {
    pub already_granted: bool,
    pub message: String,
}

#[cfg(target_os = "linux")]
const LINUX_SERIAL_GROUP: &str = "dialout";

#[tauri::command]
pub fn trigger_apply_port_permissions(app_handle: AppHandle) {
    std::thread::Builder::new()
        .name("peripheral-permissions-job".to_string())
        .spawn(move || match apply_permissions() {
            Ok(payload) => {
                app_handle.emit("peripheral-permissions-status", payload).ok();
            }
            Err(message) => {
                app_handle.emit("peripheral-permissions-error", message).ok();
            }
        })
        .ok();
}

#[cfg(target_os = "linux")]
fn apply_permissions() -> Result<PermissionsResultPayload, String> {
    use std::process::Command;

    let username = current_username()?;

    if user_in_group(&username, LINUX_SERIAL_GROUP) {
        logging::step(
            "system_permissions::apply_permissions",
            format!("{username} already in {LINUX_SERIAL_GROUP}"),
        );
        return Ok(PermissionsResultPayload {
            already_granted: true,
            message: format!(
                "Tu usuario ya pertenece al grupo '{LINUX_SERIAL_GROUP}'. Los puertos serie ya están disponibles."
            ),
        });
    }

    logging::step(
        "system_permissions::apply_permissions",
        format!("requesting elevation to add {username} to {LINUX_SERIAL_GROUP}"),
    );

    let status = Command::new("pkexec")
        .args(["usermod", "-aG", LINUX_SERIAL_GROUP, &username])
        .status()
        .map_err(|e| format!("No se pudo iniciar la solicitud de permisos (pkexec): {e}"))?;

    if !status.success() {
        return Err(
            "La solicitud de permisos fue cancelada o falló. Vuelve a intentarlo e ingresa la contraseña de administrador cuando se te pida.".to_string(),
        );
    }

    Ok(PermissionsResultPayload {
        already_granted: false,
        message: format!(
            "Permisos aplicados. Cierra sesión (o reinicia el equipo) y vuelve a abrir Galfield POS para que el acceso a puertos serie ('{LINUX_SERIAL_GROUP}') tenga efecto."
        ),
    })
}

#[cfg(target_os = "linux")]
fn current_username() -> Result<String, String> {
    use std::process::Command;

    let output = Command::new("id")
        .arg("-un")
        .output()
        .map_err(|e| format!("No se pudo determinar el usuario actual: {e}"))?;

    if !output.status.success() {
        return Err("No se pudo determinar el usuario actual.".to_string());
    }

    Ok(String::from_utf8_lossy(&output.stdout).trim().to_string())
}

#[cfg(target_os = "linux")]
fn user_in_group(username: &str, group: &str) -> bool {
    use std::process::Command;

    Command::new("id")
        .args(["-nG", username])
        .output()
        .map(|o| {
            String::from_utf8_lossy(&o.stdout)
                .split_whitespace()
                .any(|g| g == group)
        })
        .unwrap_or(false)
}

#[cfg(target_os = "windows")]
fn apply_permissions() -> Result<PermissionsResultPayload, String> {
    Ok(PermissionsResultPayload {
        already_granted: true,
        message: "En Windows los puertos COM no requieren permisos adicionales del sistema. Si un dispositivo no aparece en la lista, instala el driver USB-serie del fabricante (CH340, FTDI o PL2303) y vuelve a conectarlo.".to_string(),
    })
}

#[cfg(not(any(target_os = "linux", target_os = "windows")))]
fn apply_permissions() -> Result<PermissionsResultPayload, String> {
    Ok(PermissionsResultPayload {
        already_granted: true,
        message: "Esta plataforma no requiere pasos adicionales para acceder a los puertos.".to_string(),
    })
}
