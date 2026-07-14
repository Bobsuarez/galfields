-- ============================================================
-- Cloud API base URL as a runtime setting instead of the hardcoded
-- `http_client::DEFAULT_API_BASE_URL` Rust constant - lets one installation
-- point at a different backend (staging, a different Galfields deployment)
-- from Configuración, without a rebuild. Seeded to the same value the
-- constant used to be, so existing installs behave identically until
-- someone actually changes it.
-- ============================================================

INSERT OR IGNORE INTO app_settings (key_property, value_property) VALUES
    ('sync.api_base_url', 'https://galfields.kinforgeworks.com');
