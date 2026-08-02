-- ============================================================
-- Invoice numbering ranges per POS terminal (DIAN electronic-invoicing
-- authorized range: prefix + [range_start, range_end]). One row per
-- terminal, looked up by terminal_code (see apps/galfield-pos's
-- invoice_numbering.rs::sync_invoice_numbering, and this repo's
-- InvoiceNumberingRangeController).
-- ============================================================

CREATE TABLE invoice_numbering_ranges
(
    range_id      BIGSERIAL PRIMARY KEY,
    terminal_code VARCHAR(50) NOT NULL UNIQUE,
    prefix        VARCHAR(20) NOT NULL,
    range_start   BIGINT      NOT NULL,
    range_end     BIGINT      NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (range_end >= range_start)
);
