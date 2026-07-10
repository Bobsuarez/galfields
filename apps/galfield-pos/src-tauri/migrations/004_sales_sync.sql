-- ============================================================
-- Outbox for pushing sales to the cloud (POST /api/inventory/adjustments):
-- sync_uuid is the idempotency key sent as clientEventId, synced_at marks
-- when that push succeeded. NULL synced_at = still pending.
--
-- sync_uuid isn't declared NOT NULL - SQLite can't add a NOT NULL column
-- without a constant DEFAULT, and every value here needs to be unique.
-- `create_sale` always sets a real one on insert; enforced by application
-- code, not the schema (same tradeoff SQLite forces elsewhere in this file).
-- ============================================================

ALTER TABLE sales ADD COLUMN sync_uuid TEXT;
ALTER TABLE sales ADD COLUMN synced_at TEXT;

-- Backfill: pre-existing sales get a synthetic uuid and are marked already
-- synced, so the push queue only ever contains sales created after this
-- migration - pushing old sales retroactively could double-count stock the
-- business already reconciled some other way before this feature existed.
UPDATE sales
SET sync_uuid = lower(hex(randomblob(16))),
    synced_at = datetime('now', 'localtime')
WHERE sync_uuid IS NULL;

CREATE INDEX IF NOT EXISTS idx_sales_synced_at ON sales(synced_at);

-- How often the background loop retries pushing pending sales when offline
-- (minutes) - deliberately separate from sync.price_sync_hours, which
-- governs pulling the catalog down (a much slower, read-direction cadence).
INSERT OR IGNORE INTO app_settings (key_property, value_property) VALUES
    ('sync.sales_retry_minutes', '5');
