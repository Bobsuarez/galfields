-- ============================================================
-- Reports access codes: a 6-digit code the mobile app (Configuración →
-- Acceso a Reportes) generates on demand, which the desktop POS validates
-- before letting a cashier into the Reportes module (see
-- apps/galfield-pos's reports_access.rs and this repo's
-- ReportsAccessCodeController). Append-only: every "Generar código" click
-- inserts a new row instead of updating one, and the currently valid code
-- is always the most recently generated row — there is no expiry column,
-- since the code is meant to stay valid until a new one is generated, not
-- rotate on a timer.
-- ============================================================

CREATE TABLE reports_access_codes
(
    reports_access_code_id BIGSERIAL PRIMARY KEY,
    code                    VARCHAR(6) NOT NULL,
    generated_at            TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
