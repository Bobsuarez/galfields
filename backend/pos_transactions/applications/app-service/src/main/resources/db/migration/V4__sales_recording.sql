-- ============================================================
-- Idempotency key for POST /api/sales (SalesService), mirroring
-- stock_adjustments.client_event_id (V2__stock_adjustments.sql) but at the
-- whole-transaction level: a POS terminal retrying a sale it already
-- reported (e.g. it applied locally but never saw the response) gets the
-- same transaction back instead of a duplicate.
-- ============================================================

ALTER TABLE sales_transactions
    ADD COLUMN client_event_id VARCHAR(100) UNIQUE;

-- ============================================================
-- Placeholder employee for sales reported by POS terminals, which have no
-- real per-cashier login yet (see backend/pos's CLAUDE.md). employees.role_id
-- and employees.logo_image are declared BIGSERIAL, which Postgres makes
-- NOT NULL unconditionally - both a role and a logo row are required to
-- create any employee, even this placeholder one.
-- ============================================================

INSERT INTO attach_files (name, url, mime_type, size)
VALUES ('placeholder-employee-logo', '', 'image/png', 0);

INSERT INTO employee_roles (role_name)
VALUES ('POS Terminal');

INSERT INTO employees (first_name, last_name, username, password_hash, role_id, logo_image, is_active)
SELECT 'Terminal',
       'POS',
       'pos-terminal',
       'not-a-real-login-no-auth-endpoint-uses-this',
       (SELECT role_id FROM employee_roles WHERE role_name = 'POS Terminal'),
       (SELECT attach_files_id FROM attach_files WHERE name = 'placeholder-employee-logo'),
       TRUE;
