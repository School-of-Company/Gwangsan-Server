CREATE TABLE IF NOT EXISTS tbl_feedback (
    feedback_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    app_version VARCHAR(32) NULL,
    os_type VARCHAR(10) NULL,
    os_version VARCHAR(32) NULL,
    device_model VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL,
    answer VARCHAR(1000) NULL,
    answered_by_member_id BIGINT NULL,
    answered_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_feedback_member FOREIGN KEY (member_id) REFERENCES tbl_member (member_id),
    CONSTRAINT fk_feedback_answered_by FOREIGN KEY (answered_by_member_id) REFERENCES tbl_member (member_id)
);

CREATE TABLE IF NOT EXISTS tbl_feedback_image (
    feedback_image_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    image_id BIGINT NOT NULL,
    feedback_id BIGINT NOT NULL,
    CONSTRAINT fk_feedback_image_image FOREIGN KEY (image_id) REFERENCES tbl_image (image_id),
    CONSTRAINT fk_feedback_image_feedback FOREIGN KEY (feedback_id) REFERENCES tbl_feedback (feedback_id)
);
