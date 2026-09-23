CREATE INDEX idx_chat_room_buyer_active_hidden
    ON tbl_chat_room (buyer_id, is_active, hidden_by_buyer_at);

CREATE INDEX idx_chat_room_seller_active_hidden
    ON tbl_chat_room (seller_id, is_active, hidden_by_seller_at);

CREATE INDEX idx_chat_message_room_checked_sender
    ON tbl_chat_message (room_id, checked, sender_id);
