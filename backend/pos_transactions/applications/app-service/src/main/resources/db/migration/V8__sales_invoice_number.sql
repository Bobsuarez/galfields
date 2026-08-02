-- ============================================================
-- Snapshotted invoice number (DIAN prefix + consecutive) reported by the
-- POS terminal at sale creation — see apps/galfield-pos's
-- invoices.rs::create_sale and migration 008_invoice_snapshot.sql. Until
-- now the cloud only ever knew a sale by its own transaction_id; this is
-- what lets "Historial de facturas" (apps/galfields-mobile) show the real,
-- DIAN-authorized invoice number instead of the raw internal id.
--
-- Nullable: transactions reported before this column existed (and any
-- terminal that somehow reports without it) have no way to backfill the
-- number that was actually printed — callers must fall back to
-- transaction_id for those, same as before this migration.
-- ============================================================

ALTER TABLE sales_transactions
    ADD COLUMN invoice_prefix VARCHAR(20),
    ADD COLUMN invoice_number VARCHAR(30);
