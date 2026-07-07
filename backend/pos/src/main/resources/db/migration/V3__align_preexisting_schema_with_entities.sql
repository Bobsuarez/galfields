-- The cluster's Postgres pod bootstraps its data directory from a
-- Kubernetes ConfigMap init script that predates these Flyway migrations.
-- That script used SERIAL/INTEGER ids and native Postgres enum types
-- (a stand-in until real migrations existed), while the JPA entities in
-- this repo expect BIGINT ids/foreign keys and VARCHAR status columns.
-- These ALTERs realign that already-existing schema; on a database created
-- fresh by V1 (already BIGINT/VARCHAR) every statement below is a no-op.

ALTER TABLE categories ALTER COLUMN category_id TYPE BIGINT USING category_id::bigint;

ALTER TABLE brands ALTER COLUMN brand_id TYPE BIGINT USING brand_id::bigint;

ALTER TABLE employee_roles ALTER COLUMN role_id TYPE BIGINT USING role_id::bigint;

ALTER TABLE payment_methods ALTER COLUMN payment_method_id TYPE BIGINT USING payment_method_id::bigint;

ALTER TABLE locations ALTER COLUMN location_id TYPE BIGINT USING location_id::bigint;

ALTER TABLE employees
    ALTER COLUMN employee_id TYPE BIGINT USING employee_id::bigint,
    ALTER COLUMN role_id TYPE BIGINT USING role_id::bigint;

ALTER TABLE customers ALTER COLUMN customer_id TYPE BIGINT USING customer_id::bigint;

ALTER TABLE suppliers ALTER COLUMN supplier_id TYPE BIGINT USING supplier_id::bigint;

ALTER TABLE products
    ALTER COLUMN product_id TYPE BIGINT USING product_id::bigint,
    ALTER COLUMN category_id TYPE BIGINT USING category_id::bigint,
    ALTER COLUMN brand_id TYPE BIGINT USING brand_id::bigint,
    ADD COLUMN IF NOT EXISTS image_object_key VARCHAR(500);

ALTER TABLE product_variants
    ALTER COLUMN variant_id TYPE BIGINT USING variant_id::bigint,
    ALTER COLUMN product_id TYPE BIGINT USING product_id::bigint;

ALTER TABLE inventory
    ALTER COLUMN inventory_id TYPE BIGINT USING inventory_id::bigint,
    ALTER COLUMN variant_id TYPE BIGINT USING variant_id::bigint,
    ALTER COLUMN location_id TYPE BIGINT USING location_id::bigint;

ALTER TABLE sales_transactions
    ALTER COLUMN transaction_id TYPE BIGINT USING transaction_id::bigint,
    ALTER COLUMN employee_id TYPE BIGINT USING employee_id::bigint,
    ALTER COLUMN customer_id TYPE BIGINT USING customer_id::bigint,
    ALTER COLUMN location_id TYPE BIGINT USING location_id::bigint,
    ALTER COLUMN payment_status TYPE VARCHAR(20) USING payment_status::text;

ALTER TABLE sale_items
    ALTER COLUMN sale_item_id TYPE BIGINT USING sale_item_id::bigint,
    ALTER COLUMN transaction_id TYPE BIGINT USING transaction_id::bigint,
    ALTER COLUMN variant_id TYPE BIGINT USING variant_id::bigint;

ALTER TABLE payments
    ALTER COLUMN payment_id TYPE BIGINT USING payment_id::bigint,
    ALTER COLUMN transaction_id TYPE BIGINT USING transaction_id::bigint,
    ALTER COLUMN payment_method_id TYPE BIGINT USING payment_method_id::bigint;

ALTER TABLE purchase_orders
    ALTER COLUMN purchase_order_id TYPE BIGINT USING purchase_order_id::bigint,
    ALTER COLUMN supplier_id TYPE BIGINT USING supplier_id::bigint,
    ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

ALTER TABLE purchase_order_items
    ALTER COLUMN po_item_id TYPE BIGINT USING po_item_id::bigint,
    ALTER COLUMN purchase_order_id TYPE BIGINT USING purchase_order_id::bigint,
    ALTER COLUMN variant_id TYPE BIGINT USING variant_id::bigint;
