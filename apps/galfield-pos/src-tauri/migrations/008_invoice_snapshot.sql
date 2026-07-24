ALTER TABLE sales ADD COLUMN invoice_prefix TEXT;
ALTER TABLE sales ADD COLUMN invoice_number TEXT;

-- Backfill best-effort para ventas ya existentes: no hay forma de saber qué
-- prefijo estaba activo en el momento de cada venta pasada (nunca se guardó),
-- así que se usa el prefijo actual — es exactamente el mismo valor que ya se
-- le mostraba al usuario hasta ahora bajo el comportamiento anterior.
UPDATE sales
SET invoice_prefix = (SELECT value_property FROM app_settings WHERE key_property = 'sync.invoice_prefix'),
    invoice_number = (SELECT value_property FROM app_settings WHERE key_property = 'sync.invoice_prefix') || printf('%06d', id)
WHERE invoice_number IS NULL;
