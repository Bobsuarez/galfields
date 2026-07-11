CREATE TABLE payment_methods_images
(
    id              BIGSERIAL PRIMARY KEY,
    payment_methods BIGINT UNIQUE NOT NULL REFERENCES payment_methods (payment_method_id),
    image_product   BIGINT        NOT NULL REFERENCES attach_files (attach_files_id),
    create_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
