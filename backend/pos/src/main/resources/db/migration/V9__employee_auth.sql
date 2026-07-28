-- ============================================================
-- Employee authentication (spec 01-login-empleados-roles): terminals as
-- their own table (replacing the loose terminal_code string that
-- invoice_numbering_ranges used alone), employee <-> terminal
-- assignments, and per-role login flags for mobile/desktop. See
-- specs/01-login-empleados-roles.md and this repo's CLAUDE.md.
-- ============================================================

CREATE TABLE terminals
(
    terminal_id   BIGSERIAL PRIMARY KEY,
    terminal_code VARCHAR(50) NOT NULL UNIQUE,
    name          VARCHAR(100),
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- One terminals row per terminal_code already present in
-- invoice_numbering_ranges, so the FK swap below never loses a range.
INSERT INTO terminals (terminal_code)
SELECT DISTINCT terminal_code
FROM invoice_numbering_ranges;

ALTER TABLE invoice_numbering_ranges
    ADD COLUMN terminal_id BIGINT REFERENCES terminals (terminal_id);

UPDATE invoice_numbering_ranges r
SET terminal_id = t.terminal_id
FROM terminals t
WHERE t.terminal_code = r.terminal_code;

ALTER TABLE invoice_numbering_ranges
    ALTER COLUMN terminal_id SET NOT NULL;

-- Preserves the one-range-per-terminal invariant that terminal_code
-- UNIQUE used to enforce (InvoiceNumberingRangeRepository#findByTerminalCode
-- assumes at most one row per terminal).
ALTER TABLE invoice_numbering_ranges
    ADD CONSTRAINT uq_invoice_numbering_ranges_terminal_id UNIQUE (terminal_id);

ALTER TABLE invoice_numbering_ranges
    DROP COLUMN terminal_code;

CREATE TABLE employee_terminals
(
    employee_id BIGINT NOT NULL REFERENCES employees (employee_id),
    terminal_id BIGINT NOT NULL REFERENCES terminals (terminal_id),
    PRIMARY KEY (employee_id, terminal_id)
);

-- Replace comparing by role name ("Administrador") with explicit flags,
-- so any future role can be enabled for mobile and/or desktop login
-- without touching code.
ALTER TABLE employee_roles
    ADD COLUMN can_login_mobile  BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE employee_roles
    ADD COLUMN can_login_desktop BOOLEAN NOT NULL DEFAULT FALSE;

-- Seed roles. The pre-existing 'POS Terminal' placeholder role
-- (V4__sales_recording.sql, used only as a FK target for sales reported
-- without real cashier attribution) is left with both flags false — it
-- has no real login endpoint to use either one with.
INSERT INTO employee_roles (role_name, permissions, can_login_mobile, can_login_desktop)
VALUES ('Administrador',
        '{"pos": true, "inventario": true, "reportes": true, "sync": true}',
        TRUE, FALSE);

INSERT INTO employee_roles (role_name, permissions, can_login_mobile, can_login_desktop)
VALUES ('Cajero',
        '{"pos": true, "inventario": false, "reportes": false, "sync": true}',
        FALSE, TRUE);

-- employees.logo_image is BIGSERIAL, which Postgres makes NOT NULL
-- unconditionally (same gotcha as the V4__sales_recording.sql placeholder
-- employee) - this spec's EmployeeController doesn't take a photo upload,
-- so any employee created without one falls back to this shared row
-- (EmployeeService looks it up by name). Distinct from V4's
-- 'placeholder-employee-logo', which stays scoped to the 'pos-terminal'
-- system employee.
INSERT INTO attach_files (name, url, mime_type, size)
VALUES ('no-employee-photo', '', 'image/png', 0);

-- Bootstrap admin: with the real SecurityFilterChain active (step 7),
-- /api/employees requires an ADMIN-authority JWT, which requires an
-- Administrador employee to log in as - chicken-and-egg with no other
-- endpoint able to create the first one. Same seed-a-known-row pattern as
-- V4's 'pos-terminal'. Username 'admin', password 'admin123' (bcrypt hash
-- below, verified against Spring Security's BCryptPasswordEncoder) -
-- **rotate this password via PUT /api/employees immediately after first
-- login in any real environment.**
INSERT INTO employees (first_name, last_name, username, password_hash, role_id, logo_image, is_active)
SELECT 'Admin',
       'Inicial',
       'admin',
       '$2b$10$.e3BNYiUelOJgAivfv/QYunwG9iyWiltfQZ3wsH33yHqg7wCiglmC',
       (SELECT role_id FROM employee_roles WHERE role_name = 'Administrador'),
       (SELECT attach_files_id FROM attach_files WHERE name = 'no-employee-photo'),
       TRUE;
