-- 게시글 신고의 대상 게시글. 기존 신고 행은 NULL 로 남아 회원 신고로 해석된다.
-- FK 생성 시 MariaDB 가 product_id 인덱스를 함께 만들므로 별도 인덱스는 두지 않는다.
ALTER TABLE tbl_report
    ADD COLUMN product_id BIGINT NULL,
    ADD CONSTRAINT fk_report_product FOREIGN KEY (product_id) REFERENCES tbl_product (product_id);
