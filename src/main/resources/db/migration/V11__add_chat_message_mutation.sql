ALTER TABLE tbl_chat_message
    ADD COLUMN original_content TEXT NULL,
    ADD COLUMN edited_at DATETIME(6) NULL,
    ADD COLUMN deleted_at DATETIME(6) NULL;
