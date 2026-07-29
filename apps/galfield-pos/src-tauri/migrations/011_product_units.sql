-- ============================================================
-- Sale units with conversion factors (spec 03-unidades-venta-conversion):
-- one local `products` row per sellable presentation of a variant, same
-- "one row per sellable thing, no separate child table" pattern already
-- used for Ropa's clothing variants (see 003_product_remote_link.sql).
--
-- Siblings sharing the same physical stock (e.g. "Media"/"Completa" of the
-- same SKU) already share `remote_variant_id` - no new grouping column
-- needed, since backend/pos's product_units hangs off
-- product_variants.variant_id, not products.product_id (see this repo's
-- CLAUDE.md and specs/03-unidades-venta-conversion.md's Decisions).
--
-- `remote_product_unit_id` identifies this exact local row's cloud
-- counterpart - it's what sync.rs now upserts by (not `barcode` anymore, see
-- sync.rs's comments: a unit's barcode is optional, so it can no longer
-- serve as a reliable de-dup key on its own).
-- ============================================================

ALTER TABLE products ADD COLUMN remote_product_unit_id INTEGER;
ALTER TABLE products ADD COLUMN unit_name              TEXT NOT NULL DEFAULT 'Unidad';
ALTER TABLE products ADD COLUMN conversion_factor      INTEGER NOT NULL DEFAULT 1;

CREATE UNIQUE INDEX IF NOT EXISTS idx_products_remote_product_unit_id ON products(remote_product_unit_id);
