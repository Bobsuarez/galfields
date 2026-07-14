-- ============================================================
-- Links each local payment_method row back to its cloud paymentMethodId, so
-- sales_sync.rs can report which cloud payment method a sale used to
-- POST /api/sales (backend/pos's SalesService needs a real paymentMethodId,
-- not a name) — same pattern as products.remote_variant_id (003).
-- ============================================================

ALTER TABLE payment_method ADD COLUMN remote_payment_method_id INTEGER;
