CREATE TABLE tbl_gwangsan_ledger (
    ledger_id     BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id     BIGINT      NOT NULL,   -- FK 없음: 탈퇴 후에도 이력 보존
    amount        INT         NOT NULL,
    balance_after INT         NOT NULL,
    type          VARCHAR(32) NOT NULL,
    ref_id        BIGINT      NULL,       -- ADMIN_ADJUST는 NULL (유니크 대상 아님)
    created_at    DATETIME(6) NOT NULL,
    CONSTRAINT uk_ledger_idem UNIQUE (type, ref_id, member_id),
    INDEX idx_ledger_member (member_id, ledger_id)
);

-- 배포는 컨테이너를 멈춘 뒤 실행되고 Flyway 가 트래픽 전에 끝나므로 이관 중 동시 쓰기는 없다.
INSERT INTO tbl_gwangsan_ledger (member_id, amount, balance_after, type, ref_id, created_at)
SELECT member_id, gwangsan, gwangsan, 'OPENING_BALANCE', member_id, NOW(6)
FROM tbl_member_detail;
