package team.startup.gwangsan.adapter.persistence.member;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
@DisplayName("광산 원장 V14 운영 스키마 마이그레이션")
class GwangsanLedgerMigrationTest {
    @Container static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4");

    @Test
    void it_copies_every_balance_as_opening_balance_and_enforces_idempotency_key() throws Exception {
        try (var connection = DriverManager.getConnection(mariadb.getJdbcUrl(), mariadb.getUsername(), mariadb.getPassword());
             var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE tbl_member_detail (member_id BIGINT PRIMARY KEY, gwangsan INT NOT NULL)");
            sql.execute("INSERT INTO tbl_member_detail VALUES (1, 10000), (2, -400), (3, 0)");
        }
        Flyway flyway = Flyway.configure().dataSource(mariadb.getJdbcUrl(), mariadb.getUsername(), mariadb.getPassword())
                .baselineOnMigrate(true).baselineVersion("13").target("14").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        flyway.validate();
        try (var connection = DriverManager.getConnection(mariadb.getJdbcUrl(), mariadb.getUsername(), mariadb.getPassword());
             var sql = connection.createStatement()) {
            try (var rows = sql.executeQuery("""
                    SELECT member_id, amount, balance_after, type, ref_id FROM tbl_gwangsan_ledger ORDER BY member_id
                    """)) {
                long[][] expected = {{1, 10000}, {2, -400}, {3, 0}};
                for (long[] e : expected) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getLong("member_id")).isEqualTo(e[0]);
                    assertThat(rows.getInt("amount")).isEqualTo((int) e[1]);
                    assertThat(rows.getInt("balance_after")).isEqualTo((int) e[1]);
                    assertThat(rows.getString("type")).isEqualTo("OPENING_BALANCE");
                    assertThat(rows.getLong("ref_id")).isEqualTo(e[0]);
                }
                assertThat(rows.next()).isFalse();
            }
            // 대사 기준: 이관 직후 모든 회원의 잔액과 원장 합이 같다.
            try (var rows = sql.executeQuery("""
                    SELECT COUNT(*) FROM tbl_member_detail d LEFT JOIN tbl_gwangsan_ledger l ON l.member_id = d.member_id
                    GROUP BY d.member_id, d.gwangsan HAVING d.gwangsan <> COALESCE(SUM(l.amount), 0)
                    """)) {
                assertThat(rows.next()).isFalse();
            }
            assertThatThrownBy(() -> sql.execute("""
                    INSERT INTO tbl_gwangsan_ledger (member_id, amount, balance_after, type, ref_id, created_at)
                    VALUES (1, 1, 1, 'OPENING_BALANCE', 1, NOW(6))
                    """)).isInstanceOf(java.sql.SQLException.class);
            // 관리자 조정은 ref_id 가 NULL 이라 유니크 대상이 아니다.
            sql.execute("""
                    INSERT INTO tbl_gwangsan_ledger (member_id, amount, balance_after, type, ref_id, created_at)
                    VALUES (1, 1, 1, 'ADMIN_ADJUST', NULL, NOW(6)), (1, 1, 2, 'ADMIN_ADJUST', NULL, NOW(6))
                    """);
        }
    }
}
