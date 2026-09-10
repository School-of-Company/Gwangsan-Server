SET @old_group_concat_max_len := @@SESSION.group_concat_max_len;
SET SESSION group_concat_max_len = 65535;

SELECT COUNT(*),
       GROUP_CONCAT(
           CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
           ORDER BY INDEX_NAME SEPARATOR ', '
       ),
       COALESCE(
           SUM(CHAR_LENGTH(CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')))
               + GREATEST(COUNT(*) - 1, 0) * 2,
           0
       )
INTO @dong_unique_count, @dong_unique_drops, @dong_unique_drops_length
FROM (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_member_detail'
      AND NON_UNIQUE = 0
      AND INDEX_NAME <> 'PRIMARY'
    GROUP BY INDEX_NAME
    HAVING COUNT(*) = 1 AND MIN(COLUMN_NAME) = 'dong_id'
) AS dong_unique_indexes;

SET SESSION group_concat_max_len = @old_group_concat_max_len;

SET @guard_sql := IF(
    @dong_unique_count = 0 OR CHAR_LENGTH(@dong_unique_drops) = @dong_unique_drops_length,
    'DO 0',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''dong_id unique index list was truncated'''
);
PREPARE guard_stmt FROM @guard_sql;
EXECUTE guard_stmt;
DEALLOCATE PREPARE guard_stmt;

SET @has_dong_fk_index := EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_member_detail'
      AND NON_UNIQUE = 1
      AND INDEX_TYPE = 'BTREE'
      AND SEQ_IN_INDEX = 1
      AND COLUMN_NAME = 'dong_id'
      AND SUB_PART IS NULL
);

SET @replacement_name_exists := EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_member_detail'
      AND INDEX_NAME = 'idx_member_detail_dong_id'
);

SET @ensure_index_sql := CASE
    WHEN @dong_unique_count = 0 OR @has_dong_fk_index THEN 'DO 0'
    WHEN @replacement_name_exists THEN
        'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''idx_member_detail_dong_id exists with an incompatible definition'''
    ELSE 'CREATE INDEX idx_member_detail_dong_id ON tbl_member_detail (dong_id)'
END;
PREPARE ensure_index_stmt FROM @ensure_index_sql;
EXECUTE ensure_index_stmt;
DEALLOCATE PREPARE ensure_index_stmt;

SET @drop_unique_sql := IF(
    @dong_unique_count = 0,
    'DO 0',
    CONCAT('ALTER TABLE tbl_member_detail ', @dong_unique_drops)
);
PREPARE drop_unique_stmt FROM @drop_unique_sql;
EXECUTE drop_unique_stmt;
DEALLOCATE PREPARE drop_unique_stmt;
