-- V1 is the Flyway baseline: the schema created by Hibernate before Flyway was introduced.
-- V2+ contains all DDL changes managed by Flyway.

-- Supports chat message cursor pagination by room.
CREATE INDEX idx_chat_message_room_created_message
    ON tbl_chat_message (room_id, created_at, message_id);

-- Supports marking unread messages as read within a room.
CREATE INDEX idx_chat_message_room_checked_message
    ON tbl_chat_message (room_id, checked, message_id);

-- Supports member alert lookups and unread alert checks.
CREATE INDEX idx_alert_receipt_member_checked_alert
    ON tbl_alert_receipt (member_id, checked, alert_id);

-- Supports completed trade statistics joined by seller and filtered by status/date.
CREATE INDEX idx_trade_complete_seller_status_completed
    ON tbl_trade_complete (seller_id, status, completed_at);
