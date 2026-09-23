-- Per-participant chat room hide (soft delete). Non-null means that side
-- deleted the room from their own list; the room and its messages remain
-- untouched for the other side and for trade/reservation data.
ALTER TABLE tbl_chat_room
    ADD COLUMN hidden_by_buyer_at DATETIME NULL,
    ADD COLUMN hidden_by_seller_at DATETIME NULL;
