-- ============================================================
-- Gwangsan Performance Test - Mock Data
-- Before (6fd62c1) vs After (develop) 비교용
--
-- 삽입 순서: head_place → place → dong → member → member_detail → product → review
-- 테스트 유저 로그인: nickname=테스트1, password=password
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 기존 테스트 데이터 초기화 (선택적)
-- TRUNCATE TABLE tbl_review;
-- TRUNCATE TABLE tbl_product;
-- TRUNCATE TABLE tbl_member_detail;
-- TRUNCATE TABLE tbl_member;
-- TRUNCATE TABLE tbl_place;
-- TRUNCATE TABLE tbl_head_place;
-- TRUNCATE TABLE tbl_dong;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. 본점/지점 (tbl_head_place)
-- ============================================================
INSERT IGNORE INTO tbl_head_place (name) VALUES
('광산본점'),
('수완지점'),
('첨단지점');

-- ============================================================
-- 2. 세부 지점 (tbl_place)
-- ============================================================
INSERT IGNORE INTO tbl_place (name, head_id) VALUES
('본점1관', 1),
('본점2관', 1),
('수완1관', 2),
('수완2관', 2),
('첨단1관', 3),
('첨단2관', 3);

-- ============================================================
-- 3. 동 (tbl_dong)
-- ============================================================
INSERT IGNORE INTO tbl_dong (name) VALUES
('월곡동'),
('신창동'),
('비아동'),
('수완동'),
('흑석동'),
('첨단동'),
('운남동'),
('임곡동'),
('동산동'),
('하남동'),
('삼도동'),
('우산동'),
('선운동'),
('산정동'),
('도산동');

-- ============================================================
-- 4. 테스트 기준 유저 (로그인용)
-- nickname: 테스트1, password: password
-- BCrypt hash of "password" (cost 10) - Spring Security BCryptPasswordEncoder 호환
-- ============================================================
INSERT IGNORE INTO tbl_member
    (name, nickname, password, phone_number, recommender_id, role, member_status, joined_at)
VALUES
    ('테스트', '테스트1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
     '01000000000', NULL, 'ROLE_HEAD_ADMIN', 'ACTIVE', NOW());

INSERT IGNORE INTO tbl_member_detail
    (member_id, dong_id, gwangsan, place_id, light, description)
SELECT m.member_id, 1, 1000, 1, 80, '테스트 계정'
FROM tbl_member m WHERE m.nickname = '테스트1'
ON DUPLICATE KEY UPDATE dong_id = 1;

-- ============================================================
-- 5. 대량 회원 생성 (500명) - Stored Procedure 사용
-- ============================================================
DROP PROCEDURE IF EXISTS insert_mock_members;
DELIMITER $$

CREATE PROCEDURE insert_mock_members()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_place_id INT;
    DECLARE v_dong_id INT;
    DECLARE v_role VARCHAR(20);

    WHILE i <= 500 DO
        SET v_place_id = (i MOD 6) + 1;
        SET v_dong_id  = (i MOD 15) + 1;
        SET v_role = CASE WHEN i MOD 20 = 0 THEN 'ROLE_PLACE_ADMIN' ELSE 'ROLE_USER' END;

        INSERT IGNORE INTO tbl_member
            (name, nickname, password, phone_number, recommender_id, role, member_status, joined_at)
        VALUES (
            CONCAT('사용자', i),
            CONCAT('닉네임', i),
            '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
            CONCAT('010', LPAD(i, 8, '0')),
            NULL,
            v_role,
            'ACTIVE',
            DATE_SUB(NOW(), INTERVAL (500 - i) DAY)
        );

        SET @last_member_id = LAST_INSERT_ID();

        IF @last_member_id > 0 THEN
            INSERT IGNORE INTO tbl_member_detail
                (member_id, dong_id, gwangsan, place_id, light, description)
            VALUES (
                @last_member_id,
                v_dong_id,
                FLOOR(RAND() * 5000) + 100,
                v_place_id,
                FLOOR(RAND() * 80) + 20,
                CONCAT('소개글 ', i)
            );
        END IF;

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;
CALL insert_mock_members();
DROP PROCEDURE IF EXISTS insert_mock_members;

-- ============================================================
-- 6. 대량 상품 생성 (300개)
-- ============================================================
DROP PROCEDURE IF EXISTS insert_mock_products;
DELIMITER $$

CREATE PROCEDURE insert_mock_products()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_member_id BIGINT;
    DECLARE v_type VARCHAR(10);
    DECLARE v_mode VARCHAR(10);
    DECLARE v_status VARCHAR(15);

    WHILE i <= 300 DO
        -- 실제 존재하는 member_id 중 랜덤 선택 (1~501 범위)
        SELECT member_id INTO v_member_id FROM tbl_member ORDER BY RAND() LIMIT 1;

        SET v_type   = CASE WHEN i MOD 2 = 0 THEN 'OBJECT' ELSE 'SERVICE' END;
        SET v_mode   = CASE WHEN i MOD 3 = 0 THEN 'RECEIVER' ELSE 'GIVER' END;
        SET v_status = CASE
            WHEN i MOD 5 = 0 THEN 'COMPLETED'
            WHEN i MOD 7 = 0 THEN 'RESERVATION'
            ELSE 'ONGOING'
        END;

        INSERT INTO tbl_product
            (title, description, gwangsan, member_id, type, mode, status, created_at, updated_at)
        VALUES (
            CONCAT('상품제목 ', i),
            CONCAT('상품설명 ', i, ' - 테스트용 목업 데이터입니다.'),
            FLOOR(RAND() * 500) + 50,
            v_member_id,
            v_type,
            v_mode,
            v_status,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY),
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
        );

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;
CALL insert_mock_products();
DROP PROCEDURE IF EXISTS insert_mock_products;

-- ============================================================
-- 7. 대량 리뷰 생성 (10,000개)
-- 인덱스 효과를 보려면 특정 reviewed_id에 리뷰가 집중되어야 함
-- → 10% 멤버에게 80% 리뷰가 집중되는 편향 분포 적용
-- ============================================================
DROP PROCEDURE IF EXISTS insert_mock_reviews;
DELIMITER $$

CREATE PROCEDURE insert_mock_reviews()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_reviewer_id BIGINT;
    DECLARE v_reviewed_id BIGINT;
    DECLARE v_product_id BIGINT;
    DECLARE v_light INT;
    DECLARE total_members INT;
    DECLARE hot_member_limit INT;

    SELECT COUNT(*) INTO total_members FROM tbl_member;
    SET hot_member_limit = GREATEST(1, FLOOR(total_members * 0.1));

    WHILE i <= 10000 DO
        -- 리뷰어: 전체 회원 랜덤
        SELECT member_id INTO v_reviewer_id FROM tbl_member ORDER BY RAND() LIMIT 1;

        -- 리뷰 대상: 80% 확률로 상위 10% 회원 (hot members), 20% 확률로 전체 랜덤
        IF (i MOD 10) < 8 THEN
            SELECT member_id INTO v_reviewed_id
            FROM tbl_member
            ORDER BY member_id ASC
            LIMIT hot_member_limit
            OFFSET FLOOR(RAND() * hot_member_limit);
        ELSE
            SELECT member_id INTO v_reviewed_id FROM tbl_member ORDER BY RAND() LIMIT 1;
        END IF;

        -- reviewer != reviewed 보장
        IF v_reviewer_id = v_reviewed_id THEN
            SELECT member_id INTO v_reviewed_id
            FROM tbl_member
            WHERE member_id != v_reviewer_id
            ORDER BY RAND()
            LIMIT 1;
        END IF;

        -- 상품: 랜덤
        SELECT product_id INTO v_product_id FROM tbl_product ORDER BY RAND() LIMIT 1;

        SET v_light = FLOOR(RAND() * 5) + 1;

        INSERT INTO tbl_review (content, light, reviewer_id, reviewed_id, product_id)
        VALUES (
            CONCAT('리뷰 내용 ', i, ' - 거래가 원활했습니다.'),
            v_light,
            v_reviewer_id,
            v_reviewed_id,
            v_product_id
        );

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;
CALL insert_mock_reviews();
DROP PROCEDURE IF EXISTS insert_mock_reviews;

-- ============================================================
-- 검증 쿼리
-- ============================================================
SELECT 'tbl_member'      AS tbl, COUNT(*) AS cnt FROM tbl_member      UNION ALL
SELECT 'tbl_member_detail',      COUNT(*)         FROM tbl_member_detail UNION ALL
SELECT 'tbl_product',            COUNT(*)         FROM tbl_product       UNION ALL
SELECT 'tbl_review',             COUNT(*)         FROM tbl_review;

-- 리뷰 집중도 확인 (상위 10명의 received 리뷰 수)
SELECT reviewed_id, COUNT(*) AS review_count
FROM tbl_review
GROUP BY reviewed_id
ORDER BY review_count DESC
LIMIT 10;
