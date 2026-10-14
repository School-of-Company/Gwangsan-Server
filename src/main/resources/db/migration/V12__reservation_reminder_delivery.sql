ALTER TABLE tbl_product_reservation
    ADD COLUMN reminder_30_sent_at DATETIME NULL,
    ADD COLUMN reminder_at_sent_at DATETIME NULL;

CREATE INDEX idx_reservation_reminders
    ON tbl_product_reservation (status, scheduled_at);

UPDATE tbl_product_reservation
SET reminder_30_sent_at = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 9 HOUR)
WHERE scheduled_at <= DATE_ADD(UTC_TIMESTAMP(), INTERVAL 570 MINUTE);

UPDATE tbl_product_reservation
SET reminder_at_sent_at = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 9 HOUR)
WHERE scheduled_at <= DATE_ADD(UTC_TIMESTAMP(), INTERVAL 9 HOUR);
