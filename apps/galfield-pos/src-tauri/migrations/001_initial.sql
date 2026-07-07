-- ============================================================
-- Galfields POS — Initial schema
-- External connection (DBeaver): jdbc:sqlite:/home/<user>/.local/share/com.galfield.pos/galfield.db
-- ============================================================

PRAGMA journal_mode = WAL;
PRAGMA foreign_keys = ON;

-- ------------------------------------------------------------
-- Categories
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL UNIQUE,
    description TEXT,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);


-- ------------------------------------------------------------
-- Products
-- SQLite type notes:
--   TEXT replaces VARCHAR(n)  — SQLite ignores length constraints
--   REAL replaces DECIMAL     — 8-byte IEEE 754 float
--   INTEGER 0/1 replaces BOOLEAN
--   TEXT replaces TIMESTAMP   — stored as ISO-8601 string
--   ON UPDATE is handled via trigger below
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode        TEXT    UNIQUE,
    product_name   TEXT    NOT NULL,
    unit_price     REAL    NOT NULL CHECK (unit_price >= 0),
    category       TEXT,
    is_active      INTEGER NOT NULL DEFAULT 1,
    image_path     TEXT    NOT NULL DEFAULT '',
    image_hash     TEXT    NOT NULL DEFAULT '',
    stock_quantity REAL    NOT NULL DEFAULT 0,
    last_sync_at   TEXT,
    created_at     TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at     TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- Emulate ON UPDATE CURRENT_TIMESTAMP (not native in SQLite)
CREATE TRIGGER IF NOT EXISTS products_set_updated_at
AFTER UPDATE ON products
FOR EACH ROW
BEGIN
    UPDATE products
    SET    updated_at = datetime('now', 'localtime')
    WHERE  id = NEW.id;
END;

CREATE INDEX IF NOT EXISTS idx_products_barcode  ON products(barcode);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);

-- ------------------------------------------------------------
-- Sales (header)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sales (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    subtotal       REAL    NOT NULL DEFAULT 0,
    discount       REAL    NOT NULL DEFAULT 0,
    total          REAL    NOT NULL DEFAULT 0,
    payment_method INTEGER NOT NULL REFERENCES payment_method(id),
    notes          TEXT,
    amount_received REAL NOT NULL DEFAULT 0,
    change_due       REAL NOT NULL DEFAULT 0,
    created_at     TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- ------------------------------------------------------------
-- Sale line items
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sale_items (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_id      INTEGER NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    product_id   INTEGER NOT NULL REFERENCES products(id),
    quantity     INTEGER NOT NULL CHECK (quantity > 0),
    unit_price   REAL    NOT NULL CHECK (unit_price >= 0),
    subtotal     REAL    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sale_items_sale ON sale_items(sale_id);

-- ------------------------------------------------------------
-- Pending sales (saved mid-session, waiting to be resumed)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pending_sales (
    id          TEXT    PRIMARY KEY,
    label       TEXT    NOT NULL,
    icon_key    TEXT    NOT NULL DEFAULT 'user',
    items_json  TEXT    NOT NULL DEFAULT '[]',
    subtotal    REAL    NOT NULL DEFAULT 0,
    discount    REAL    NOT NULL DEFAULT 0,
    total       REAL    NOT NULL DEFAULT 0,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- ------------------------------------------------------------
-- Application settings  (key-value store)
-- key_property format: "<section>.<field>"  e.g. "general.store_name"
-- value_property: always stored as TEXT; booleans as "true"/"false"
-- additional_config: optional JSON for structured metadata
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_settings (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    key_property      TEXT    NOT NULL UNIQUE,
    value_property    TEXT    NOT NULL,
    additional_config TEXT    DEFAULT NULL,
    created_at        TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at        TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_app_settings_key ON app_settings(key_property);

-- ------------------------------------------------------------
-- Default seed data — categories
-- ------------------------------------------------------------
INSERT OR IGNORE INTO categories (name, description) VALUES
    ('General',    'Default category'),
    ('Beverages',  'Cold and hot drinks'),
    ('Food',       'Food and snacks'),
    ('Dairy',      'Dairy products'),
    ('Cleaning',   'Cleaning supplies');

-- ------------------------------------------------------------
-- Default seed data — app_settings
-- Maps 1:1 with ConfigSettings in src/features/configuration/composables/useConfig.ts
-- ------------------------------------------------------------
INSERT OR IGNORE INTO app_settings (key_property, value_property) VALUES
    -- Section: general
    ('general.store_name',   'Galfields'),
    ('general.tax_id',       '900123456-7'),
    ('general.language',     'es'),
    ('general.currency',     'COP'),
    ('general.timezone',     'America/Bogota'),
    ('general.date_format',  'DD/MM/YYYY'),

    -- Section: store
    ('store.name',           'Galfields'),
    ('store.address',        'Calle 51 B Sur # 89A-71, Bogotá'),
    ('store.phone',          '+57 302 809 1893'),
    ('store.email',          'crystal11041@gmail.com'),
    ('store.slogan',         'Tu punto de venta inteligente'),

    -- Section: defaults
    ('defaults.seller',               'Cajero 1'),
    ('defaults.customer',             'Consumidor Final'),
    ('defaults.main_category',        'beverages'),
    ('defaults.tax_policy',           'no_tax'),
    ('defaults.print_receipt',        'false'),
    ('defaults.email_receipt',        'false'),
    ('defaults.round_prices',         'true'),
    ('defaults.email_notifications',  'false'),
    ('defaults.validate_stock',       'true'),
    ('defaults.invoice_archive_folder', ''),

    -- Section: sync
    ('sync.backup_interval',   '30min'),
    ('sync.price_sync_hours',  '24'),
    ('sync.invoice_prefix',    'FAC-'),

    -- Section: styles
    -- bg_color    → --color-bg, --color-surface     (main background)
    -- light_bg    → --color-surface-2, surface-3    (cards, panels)
    -- light_text  → --color-text                    (main text color)
    ('styles.theme',            'dark'),
    ('styles.primary_color',    '#F28D35'),
    ('styles.bg_color',         '#865e3c'),
    ('styles.light_bg',         '#deddda'),
    ('styles.secondary_text',   '#000000'),
    ('styles.light_text',       '#1a1a1a'),

    -- Section: peripherals
    -- Values are OS device paths (/dev/ttyUSB0 on Linux, COM3 on Windows)
    -- Empty string means device is not configured
    ('peripherals.printer_port',        ''),
    ('peripherals.printer_paper_width', '80mm'),
    ('peripherals.barcode_port',      ''),
    ('peripherals.cash_drawer_port',  ''),
    ('peripherals.camera_device',     ''),
    ('peripherals.fingerprint_port',  '');
