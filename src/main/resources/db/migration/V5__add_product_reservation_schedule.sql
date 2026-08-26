ALTER TABLE tbl_product_reservation
    ADD COLUMN scheduled_at DATETIME NULL,
    ADD COLUMN location VARCHAR(100) NULL;
