-- ============================================================
-- Sale cancellation: cancelled_at is a separate axis from payment_status
-- (a Paid sale that gets voided is still "was Paid", just cancelled now).
-- See SalesService#cancelSale / SalesController's /cancel endpoints.
-- ============================================================

ALTER TABLE sales_transactions
    ADD COLUMN cancelled_at TIMESTAMP;
