-- (product_id, status) 유니크 제약은 "상품당 상태별 1행"을 강제해서,
-- 같은 상품을 두 번째로 취소할 때 (product_id, CANCELLED) 중복으로 500이 발생했다.
-- 동시에 활성(PENDING) 예약이 1건이라는 규칙은 product.status 체크로 애플리케이션에서 보장한다.

-- FK(product_id)가 이 유니크 인덱스를 타고 있을 수 있어, 대체 인덱스를 먼저 만든 뒤 드롭한다.
CREATE INDEX idx_product_reservation_product_status
    ON tbl_product_reservation (product_id, status);

-- Flyway 도입 전 Hibernate가 붙인 이름(UK...)은 환경마다 달라 information_schema에서 찾아 드롭한다.
SET @uk := (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_product_reservation'
      AND NON_UNIQUE = 0
      AND INDEX_NAME <> 'PRIMARY'
    GROUP BY INDEX_NAME
    HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'product_id,status'
    LIMIT 1
);
SET @sql := IF(@uk IS NULL, 'DO 0',
               CONCAT('ALTER TABLE tbl_product_reservation DROP INDEX `', @uk, '`'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
