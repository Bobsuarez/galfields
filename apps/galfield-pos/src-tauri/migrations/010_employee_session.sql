-- ============================================================
-- Cached employee session (spec 01-login-empleados-roles). One row per
-- installation — CHECK (id = 1) forces a single active session, so logging
-- a new cashier in (or logging out) replaces the row instead of
-- accumulating history. jwt/expires_at are what auth.rs writes on login and
-- reads back for get_session/logout; expires_at is always the next
-- midnight in America/Bogota (see auth.rs::login), computed in SQL from
-- SQLite's own UTC 'now' shifted -5 hours (Colombia has no DST, so this is
-- exact — no timezone database/crate needed).
-- ============================================================

CREATE TABLE IF NOT EXISTS employee_session (
    id            INTEGER PRIMARY KEY CHECK (id = 1),
    employee_id   INTEGER NOT NULL,
    username      TEXT    NOT NULL,
    role_name     TEXT    NOT NULL,
    permissions   TEXT    NOT NULL,
    jwt           TEXT    NOT NULL,
    expires_at    TEXT    NOT NULL
);
