CREATE TABLE tbl_feedback (
    feedback_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    type ENUM('BUG', 'SUGGESTION', 'TRADE', 'ACCOUNT', 'ETC') NOT NULL,
    content VARCHAR(1000) NOT NULL,
    app_version VARCHAR(255) NULL,
    os_type ENUM('IOS', 'ANDROID') NOT NULL,
    os_version VARCHAR(255) NULL,
    device_model VARCHAR(255) NULL,
    status ENUM('RECEIVED', 'IN_PROGRESS', 'DONE') NOT NULL,
    answer TEXT NULL,
    answered_by BIGINT NULL,
    answered_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (feedback_id),
    CONSTRAINT fk_feedback_member FOREIGN KEY (member_id) REFERENCES tbl_member (member_id),
    CONSTRAINT fk_feedback_admin FOREIGN KEY (answered_by) REFERENCES tbl_member (member_id),
    INDEX idx_feedback_member_cursor (member_id, feedback_id),
    INDEX idx_feedback_status_type_cursor (status, type, feedback_id)
);

CREATE TABLE tbl_feedback_image (
    feedback_image_id BIGINT NOT NULL AUTO_INCREMENT,
    feedback_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    PRIMARY KEY (feedback_image_id),
    CONSTRAINT uk_feedback_image UNIQUE (feedback_id, image_id),
    CONSTRAINT fk_feedback_image_feedback FOREIGN KEY (feedback_id) REFERENCES tbl_feedback (feedback_id),
    CONSTRAINT fk_feedback_image_image FOREIGN KEY (image_id) REFERENCES tbl_image (image_id)
);

ALTER TABLE tbl_alert MODIFY COLUMN alert_type
    ENUM('NOTICE', 'TRADE_COMPLETE', 'TRADE_COMPLETE_REJECT', 'OTHER_MEMBER_TRADE_COMPLETE',
         'RECOMMENDER', 'REVIEW', 'TRADE_CANCEL', 'TRADE_CANCEL_REJECT', 'REPORT', 'REPORT_REJECT',
         'FEEDBACK_ANSWER') NOT NULL;
