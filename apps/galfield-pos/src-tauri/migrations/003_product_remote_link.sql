-- ============================================================
-- Links each local product row back to its cloud variant id, so future
-- push-sync work (reporting sold stock back to the cloud) can address the
-- right remote record directly instead of guessing from name/barcode.
-- ============================================================

ALTER TABLE products ADD COLUMN remote_variant_id INTEGER;

CREATE INDEX IF NOT EXISTS idx_products_remote_variant_id ON products(remote_variant_id);
