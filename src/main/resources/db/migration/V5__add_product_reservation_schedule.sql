ALTER TABLE tbl_product_reservation
    ADD COLUMN scheduled_at DATETIME NULL,
    ADD COLUMN place_name VARCHAR(100) NULL,
    ADD COLUMN address VARCHAR(255) NULL,
    ADD COLUMN latitude DOUBLE NULL,
    ADD COLUMN longitude DOUBLE NULL;
