package team.startup.gwangsan.adapter.persistence.feedback;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
@DisplayName("문의 V13 운영 스키마 마이그레이션")
class FeedbackMigrationTest {
    @Container static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4");

    @Test
    void it_migrates_v12_schema_preserving_alerts_and_enforcing_feedback_foreign_keys() throws Exception {
        try (var connection = DriverManager.getConnection(mariadb.getJdbcUrl(), mariadb.getUsername(), mariadb.getPassword());
             var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE tbl_member (member_id BIGINT PRIMARY KEY)");
            sql.execute("CREATE TABLE tbl_image (image_id BIGINT PRIMARY KEY)");
            sql.execute("""
                    CREATE TABLE tbl_alert (alert_id BIGINT PRIMARY KEY,
                        alert_type ENUM('NOTICE','TRADE_COMPLETE','TRADE_COMPLETE_REJECT','OTHER_MEMBER_TRADE_COMPLETE',
                        'RECOMMENDER','REVIEW','TRADE_CANCEL','TRADE_CANCEL_REJECT','REPORT','REPORT_REJECT') NOT NULL)
                    """);
            sql.execute("INSERT INTO tbl_member VALUES (1), (2)");
            sql.execute("INSERT INTO tbl_image VALUES (5)");
            sql.execute("INSERT INTO tbl_alert VALUES (1, 'NOTICE')");
        }
        Flyway flyway = Flyway.configure().dataSource(mariadb.getJdbcUrl(), mariadb.getUsername(), mariadb.getPassword())
                .baselineOnMigrate(true).baselineVersion("12").target("13").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        flyway.validate();
        try (var connection = DriverManager.getConnection(mariadb.getJdbcUrl(), mariadb.getUsername(), mariadb.getPassword());
             var sql = connection.createStatement()) {
            sql.execute("""
                    INSERT INTO tbl_feedback (member_id,type,content,os_type,status,answer,answered_by,answered_at,created_at,updated_at)
                    VALUES (1,'BUG','문의','IOS','DONE','답변',2,NOW(6),NOW(6),NOW(6))
                    """);
            sql.execute("INSERT INTO tbl_feedback_image(feedback_id,image_id) VALUES(1,5)");
            sql.execute("INSERT INTO tbl_alert VALUES (2, 'FEEDBACK_ANSWER')");
            try (var rows = sql.executeQuery("SELECT alert_type FROM tbl_alert ORDER BY alert_id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("NOTICE");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("FEEDBACK_ANSWER");
            }
            assertThatThrownBy(() -> sql.execute("DELETE FROM tbl_member WHERE member_id=1"))
                    .isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> sql.execute("DELETE FROM tbl_member WHERE member_id=2"))
                    .isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> sql.execute("DELETE FROM tbl_image WHERE image_id=5"))
                    .isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> sql.execute("INSERT INTO tbl_feedback_image(feedback_id,image_id) VALUES(1,5)"))
                    .isInstanceOf(java.sql.SQLException.class);
        }
    }
}
