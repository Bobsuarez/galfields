-- ============================================================
-- Stock adjustments reported by POS terminals (a sale decrements stock; a
-- future return/manual correction could increment it). Idempotent per
-- (client_event_id, variant_id): a terminal retrying a batch it already
-- applied locally (e.g. it never saw the response) gets a no-op replay
-- instead of double-decrementing inventory. Scoped to the same hardcoded
-- default location every other inventory write uses today (see
-- ProductService.DEFAULT_LOCATION_NAME) - multi-location POS is a bigger,
-- separate change.
-- ============================================================

CREATE TABLE stock_adjustments
(
    adjustment_id      BIGSERIAL PRIMARY KEY,
    client_event_id    VARCHAR(100) NOT NULL,
    variant_id         BIGINT       NOT NULL REFERENCES product_variants (variant_id),
    location_id        BIGINT       NOT NULL REFERENCES locations (location_id),
    quantity_delta     INT          NOT NULL,
    resulting_quantity INT          NOT NULL,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (client_event_id, variant_id)
);

CREATE INDEX idx_stock_adjustments_variant ON stock_adjustments (variant_id);
