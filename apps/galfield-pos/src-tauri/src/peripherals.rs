use std::fs;
use serialport;

#[tauri::command]
pub fn list_serial_ports() -> Vec<String> {
    let mut ports: Vec<String> = Vec::new();

    // 1. Obtener puertos usando la librería nativa multiplataforma
    if let Ok(available_ports) = serialport::available_ports() {
        for p in available_ports {
            // En Linux, filtramos los molestos puertos "ttyS" que son virtuales o internos
            #[cfg(target_os = "linux")]
            if p.port_name.starts_with("/dev/ttyS") {
                continue;
            }
            
            ports.push(p.port_name);
        }
    }

    // 2. SOPORTE ESPECIAL PARA IMPRESORAS USB EN LINUX
    // Las impresoras POS nativas en Linux NO se abren como puertos seriales tradicionales,
    // se mapean en /dev/usb/lp0, lp1, etc. Las agregamos manualmente si existen.
    #[cfg(target_os = "linux")]
    {
        if let Ok(entries) = fs::read_dir("/dev/usb") {
            for entry in entries.flatten() {
                if let Ok(name) = entry.file_name().into_string() {
                    if name.starts_with("lp") {
                        ports.push(format!("/dev/usb/{}", name));
                    }
                }
            }
        }
    }

    // Ordenar la lista final para que el dropdown en tu frontend sea agradable
    ports.sort();
    ports
}

/// Returns available video/camera devices on the current OS.
#[tauri::command]
pub fn list_video_devices() -> Vec<String> {
    let mut devices: Vec<String> = Vec::new();

    #[cfg(target_os = "linux")]
    {
        if let Ok(entries) = fs::read_dir("/dev") {
            for entry in entries.flatten() {
                if let Ok(name) = entry.file_name().into_string() {
                    if name.starts_with("video") {
                        devices.push(format!("/dev/{}", name));
                    }
                }
            }
        }
        devices.sort();
    }

    // macOS and Windows camera enumeration requires AVFoundation / DirectShow.
    // Return an empty list on those platforms for now.
    devices
}
