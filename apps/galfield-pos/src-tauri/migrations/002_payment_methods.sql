-- ============================================================
-- Payment methods as data instead of a hardcoded list, so adding one
-- (e.g. a new wallet app) is a row insert, not a code change.
-- A separate, versioned migration (not folded into 001_initial.sql) so it
-- still runs on databases that already recorded "001_initial" as applied.
-- ============================================================

CREATE TABLE IF NOT EXISTS payment_method (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL UNIQUE,
    url         TEXT    NULL,
    description TEXT,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

INSERT OR IGNORE INTO payment_method (name, description) VALUES
    ('Efectivo',    'Pago en efectivo'),
    ('Nequi',       'Billetera digital Nequi'),
    ('Daviplata',   'Billetera digital Daviplata'),
    ('BreB',        'Pagos interoperables Bre-B'),
    ('Dale',        'Billetera digital Dale'),
    ('Bancolombia', 'Transferencia Bancolombia a la Mano');
