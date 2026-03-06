-- ============================================================
-- DB 레벨 인덱스 적중률 분석 쿼리
-- MariaDB / MySQL 기준
-- JMeter 테스트 전/후로 실행해서 비교
-- ============================================================

-- ============================================================
-- 1. 인덱스 사용 여부 확인 (EXPLAIN)
-- ============================================================

-- Review 조회: reviewed_id 인덱스 적중 확인
-- BEFORE: full scan 예상 / AFTER: idx_review_reviewed_id 사용 예상
EXPLAIN SELECT r.review_id, r.content, r.light, r.reviewer_id, r.product_id
FROM tbl_review r
WHERE r.reviewed_id = 1;

EXPLAIN SELECT r.review_id, r.content, r.light, r.reviewer_id, r.product_id
FROM tbl_review r
WHERE r.reviewer_id = 1;

-- Member 전체 조회: joined_at, place_id 인덱스 적중 확인
EXPLAIN SELECT m.member_id, m.nickname, m.name, m.phone_number, m.role, m.member_status, m.joined_at
FROM tbl_member m
ORDER BY m.joined_at DESC
LIMIT 20;

EXPLAIN SELECT m.member_id, m.nickname, m.name, m.phone_number, m.role, m.member_status, m.joined_at,
               md.place_id
FROM tbl_member m
JOIN tbl_member_detail md ON m.member_id = md.member_id
WHERE md.place_id = 1
ORDER BY m.joined_at DESC
LIMIT 20;

-- ============================================================
-- 2. 인덱스 목록 확인
-- ============================================================
SHOW INDEX FROM tbl_review;
SHOW INDEX FROM tbl_member;
SHOW INDEX FROM tbl_member_detail;

-- ============================================================
-- 3. 쿼리 실행 통계 (performance_schema 활성화 필요)
-- ============================================================

-- performance_schema 활성화 여부 확인
SELECT @@performance_schema;

-- 최근 슬로우 쿼리 확인 (slow_query_log 활성화 필요)
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 0.1;  -- 100ms 이상 쿼리 로깅
-- SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';

-- 테이블별 풀스캔 vs 인덱스 스캔 통계
SELECT
    OBJECT_SCHEMA,
    OBJECT_NAME,
    COUNT_READ,
    COUNT_WRITE,
    COUNT_FETCH,
    SUM_TIMER_WAIT / 1000000000 AS total_wait_ms
FROM performance_schema.table_io_waits_summary_by_table
WHERE OBJECT_SCHEMA = DATABASE()
  AND OBJECT_NAME IN ('tbl_review', 'tbl_member', 'tbl_member_detail')
ORDER BY total_wait_ms DESC;

-- 인덱스별 사용 횟수
SELECT
    OBJECT_NAME   AS table_name,
    INDEX_NAME,
    COUNT_FETCH   AS fetch_count,
    COUNT_INSERT  AS insert_count,
    SUM_TIMER_FETCH / 1000000000 AS fetch_wait_ms
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE OBJECT_SCHEMA = DATABASE()
  AND OBJECT_NAME IN ('tbl_review', 'tbl_member', 'tbl_member_detail')
ORDER BY fetch_count DESC;

-- ============================================================
-- 4. InnoDB 버퍼 풀 통계 (캐시 히트율)
-- ============================================================
SELECT
    (SELECT variable_value FROM information_schema.global_status
     WHERE variable_name = 'Innodb_buffer_pool_read_requests') AS buffer_pool_reads,
    (SELECT variable_value FROM information_schema.global_status
     WHERE variable_name = 'Innodb_buffer_pool_reads') AS disk_reads,
    ROUND(
        (1 - (SELECT variable_value FROM information_schema.global_status
              WHERE variable_name = 'Innodb_buffer_pool_reads') /
             (SELECT variable_value FROM information_schema.global_status
              WHERE variable_name = 'Innodb_buffer_pool_read_requests')) * 100, 2
    ) AS buffer_hit_rate_pct;

-- ============================================================
-- 5. 통계 초기화 (테스트 전 실행)
-- ============================================================
-- FLUSH STATUS;
-- TRUNCATE performance_schema.table_io_waits_summary_by_table;
-- TRUNCATE performance_schema.table_io_waits_summary_by_index_usage;

-- ============================================================
-- 6. 데이터 분포 확인 (인덱스 선택도)
-- ============================================================

-- reviewed_id 분포 (상위 10개)
SELECT reviewed_id, COUNT(*) AS cnt
FROM tbl_review
GROUP BY reviewed_id
ORDER BY cnt DESC
LIMIT 10;

-- reviewer_id 분포 (상위 10개)
SELECT reviewer_id, COUNT(*) AS cnt
FROM tbl_review
GROUP BY reviewer_id
ORDER BY cnt DESC
LIMIT 10;

-- place_id별 member 분포
SELECT md.place_id, p.name AS place_name, COUNT(*) AS member_cnt
FROM tbl_member_detail md
JOIN tbl_place p ON md.place_id = p.place_id
GROUP BY md.place_id, p.name
ORDER BY member_cnt DESC;

-- 가입일 분포 (월별)
SELECT DATE_FORMAT(joined_at, '%Y-%m') AS month, COUNT(*) AS cnt
FROM tbl_member
GROUP BY month
ORDER BY month;
