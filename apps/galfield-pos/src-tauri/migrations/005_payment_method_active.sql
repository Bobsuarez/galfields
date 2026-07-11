-- ============================================================
-- Payment methods can be deactivated from the cloud (mirrors products'
-- is_active). Unlike `categories` — nothing local references categories.id,
-- so syncing that table is a safe delete-and-reinsert — `sales.payment_method`
-- has a real foreign key into this table. Deleting a row a past sale points
-- to would violate that FK, so catalog_sync.rs never deletes here: it
-- upserts by name and flips is_active off for anything the cloud stopped
-- returning, same fix as products' "stale sweep" in sync.rs.
-- ============================================================

ALTER TABLE payment_method ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1;
