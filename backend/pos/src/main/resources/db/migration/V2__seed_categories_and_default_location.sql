-- Seed data the mobile provisioning app relies on: its fixed category enum
-- must resolve against real category_id rows, and product stock needs a
-- location to attach an Inventory row to until multi-location support exists.

INSERT INTO categories (name) VALUES
    ('Bebidas'),
    ('Snacks'),
    ('Lácteos'),
    ('Panadería'),
    ('Limpieza'),
    ('Otro');

INSERT INTO locations (name) VALUES ('Bodega Principal');
