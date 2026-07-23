//! System-level peripheral permissions (serial ports + USB printer) — a
//! one-shot action, not tied to any single device (barcode/printer/
//! cash-drawer/etc. all share the same OS-level access requirement, just
//! via different groups — see below). Dispatched with
//! `trigger_apply_port_permissions` and
//! reported via `peripheral-permissions-status`/`peripheral-permissions-error`
//! events, same fire-and-listen convention as the other `trigger_*` commands
//! (see "Peripheral event model" in CLAUDE.md).
//!
//! On Linux, reading/writing `/dev/ttyUSB*`/`/dev/ttyACM*` (barcode scanner,
//! RS232-over-USB peripherals) requires the OS user to belong to the
//! `dialout` group, and USB-class printers exposed as `/dev/usb/lp*` (the
//! node that registers a directly-connected receipt printer — see field
//! reports of `trigger_print_invoice` failing silently because of this)
//! require the `lp` group instead — udev's default rules on Debian/Ubuntu
//! (and derivatives, which is what field installs run) own that device node
//! as `root:lp`, a completely separate group from `dialout`. The installer
//! is supposed to run `usermod -aG dialout,lp <user>` but sometimes skips
//! it, or an older install only ever added `dialout`. This lets the user
//! trigger that same command from inside the app instead, elevated via
//! `pkexec` (the GUI polkit prompt) since a GUI app has no TTY for `sudo`.
//! Windows ports don't have an equivalent OS permission gate — a port that
//! doesn't show up there is almost always a missing USB driver (CH340/FTDI/
//! PL2303 for serial, the manufacturer's printer driver for USB printers),
//! which this command can't install, so it just reports that instead of
//! running anything.

use tauri::{AppHandle, Emitter};

use crate::logging;

#[derive(serde::Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsResultPayload {
    pub already_granted: bool,
    pub message: String,
}

// `(group, human-readable device description)` — every group the OS user
// needs for the peripherals this app talks to. Add a new entry here (and to
// the udev-owned device it corresponds to) if a future peripheral class
// needs another group; `apply_permissions` grants whatever subset is missing
// in one `usermod` call rather than one call per group.
#[cfg(target_os = "linux")]
const LINUX_REQUIRED_GROUPS: &[(&str, &str)] = &[
    ("dialout", "puertos serie (lector de código de barras)"),
    ("lp", "impresora USB"),
];

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

    let missing: Vec<&(&str, &str)> = LINUX_REQUIRED_GROUPS
        .iter()
        .filter(|(group, _)| !user_in_group(&username, group))
        .collect();

    if missing.is_empty() {
        let groups = LINUX_REQUIRED_GROUPS
            .iter()
            .map(|(g, _)| *g)
            .collect::<Vec<_>>()
            .join(", ");
        logging::step(
            "system_permissions::apply_permissions",
            format!("{username} already in {groups}"),
        );
        return Ok(PermissionsResultPayload {
            already_granted: true,
            message: "Tu usuario ya pertenece a los grupos necesarios. Los puertos serie y la impresora USB ya están disponibles.".to_string(),
        });
    }

    let missing_groups = missing.iter().map(|(g, _)| *g).collect::<Vec<_>>().join(",");
    let missing_descriptions = missing.iter().map(|(_, d)| *d).collect::<Vec<_>>().join(", ");

    logging::step(
        "system_permissions::apply_permissions",
        format!("requesting elevation to add {username} to {missing_groups}"),
    );

    let status = Command::new("pkexec")
        .args(["usermod", "-aG", &missing_groups, &username])
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
            "Permisos aplicados ({missing_descriptions}). Cierra sesión (o reinicia el equipo) y vuelve a abrir Galfield POS para que el acceso tenga efecto."
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
