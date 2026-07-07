mod db;
mod invoices;
mod payment_methods;
mod pending_sales;
mod peripheral_manager;
mod peripherals;
mod products;
mod reports;
mod settings;

use std::collections::HashMap;
use std::sync::{atomic::AtomicBool, Arc, Mutex};
use tauri::Manager;

use db::Database;

pub struct AppState {
    pub db: Mutex<Database>,
    /// Stores stop signals for active peripheral listener threads.
    /// Key: device type string ("barcode", "fingerprint", …)
    pub peripheral_stops: Mutex<HashMap<String, Arc<AtomicBool>>>,
    /// One lock per physical port, so one-shot peripheral actions that share
    /// the same port (e.g. the printer and the cash drawer wired through its
    /// RJ11) serialize instead of both opening the device file at once.
    /// Key: port path (e.g. "/dev/usb/lp0"). See `invoices::get_port_lock`.
    pub port_locks: Mutex<HashMap<String, Arc<Mutex<()>>>>,
}

#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_dialog::init())
        .setup(|app| {
            let data_dir = app
                .path()
                .app_data_dir()
                .expect("Could not resolve app data directory");

            let database = Database::init(data_dir.clone())
                .unwrap_or_else(|e| panic!("Failed to initialize database: {}", e));

            println!("[DB] Database ready at: {:?}", database.path);

            app.manage(AppState {
                db: Mutex::new(database),
                peripheral_stops: Mutex::new(HashMap::new()),
                port_locks: Mutex::new(HashMap::new()),
            });

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            greet,
            settings::get_settings,
            settings::save_settings,
            pending_sales::save_pending_sale,
            pending_sales::get_pending_sales,
            pending_sales::delete_pending_sale,
            peripherals::list_serial_ports,
            peripherals::list_video_devices,
            payment_methods::list_payment_methods,
            reports::get_sales_summary,
            reports::get_top_products,
            reports::get_financial_summary,
            products::get_low_stock_products,
            peripheral_manager::start_peripheral_listener,
            peripheral_manager::stop_peripheral_listener,
            invoices::create_sale,
            invoices::trigger_print_invoice,
            invoices::trigger_open_cash_drawer,
            invoices::save_invoice_pdf,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
