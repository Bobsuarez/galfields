-- ============================================================
-- Sale units with conversion factors (spec 03-unidades-venta-conversion):
-- a product_variant can be sold under multiple presentations sharing the
-- same physical stock (e.g. "Media"/"Completa" cajetilla of the same SKU),
-- each with its own name/factor/price/barcode. Hangs off product_variants,
-- not products directly, since that's where price/barcode/stock already
-- live in this schema (see specs/03-unidades-venta-conversion.md's
-- Decisions and this repo's CLAUDE.md "Notable shapes" section).
-- ============================================================

CREATE TABLE product_units
(
    product_unit_id   BIGSERIAL PRIMARY KEY,
    variant_id        BIGINT         NOT NULL REFERENCES product_variants (variant_id),
    unit_name         VARCHAR(50)    NOT NULL,
    conversion_factor INTEGER        NOT NULL CHECK (conversion_factor >= 1),
    unit_price        DECIMAL(15, 2) NOT NULL,
    barcode           VARCHAR(100) UNIQUE,
    is_base           BOOLEAN        NOT NULL DEFAULT FALSE,
    is_active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Exactly one base unit per variant (factor = 1, the unit in which
-- inventory.quantity_on_hand is already counted today).
CREATE UNIQUE INDEX uq_product_units_base ON product_units (variant_id) WHERE is_base;

-- Backfill: every existing variant gets its own automatic base unit, so
-- nothing stops selling the day of this deploy.
INSERT INTO product_units (variant_id, unit_name, conversion_factor, unit_price, barcode, is_base)
SELECT variant_id, 'Unidad', 1, price, barcode, TRUE
FROM product_variants;

-- sale_items gains the snapshot of which unit was sold (same criterion the
-- rest of a sale already uses - price/invoice prefix are frozen at sale
-- time). variant_id is kept as-is: it still identifies the physical stock
-- row; product_unit_id/unit_name/conversion_factor say which presentation
-- of that variant was actually sold.
ALTER TABLE sale_items
    ADD COLUMN product_unit_id   BIGINT REFERENCES product_units (product_unit_id),
    ADD COLUMN unit_name         VARCHAR(50) NOT NULL DEFAULT 'Unidad',
    ADD COLUMN conversion_factor INTEGER     NOT NULL DEFAULT 1;
