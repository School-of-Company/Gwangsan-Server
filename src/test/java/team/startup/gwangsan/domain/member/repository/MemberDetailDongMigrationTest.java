package team.startup.gwangsan.domain.member.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.place.entity.Place;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("MemberDetail Dong legacy schema migration")
class MemberDetailDongMigrationTest {

    @Container
    static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4");

    private static final AtomicInteger schemaSequence = new AtomicInteger();
    private static final Set<String> schemas = new LinkedHashSet<>();

    @BeforeAll
    static void reportContainer() {
        System.out.printf("QA_RESOURCE_CREATED container=%s image=%s%n",
                mariadb.getContainerId(), mariadb.getDockerImageName());
    }

    @AfterAll
    static void dropSchemas() throws SQLException {
        try (Connection connection = adminConnection(); Statement statement = connection.createStatement()) {
            for (String schema : schemas) {
                statement.execute("DROP DATABASE `" + schema + "`");
                System.out.printf("QA_RESOURCE_CLEANED schema=%s%n", schema);
            }
        }
        System.out.printf("QA_RESOURCE_CLEANUP container=%s managed_by=testcontainers%n", mariadb.getContainerId());
    }

    @Test
    @DisplayName("sole FK-supporting legacy uniques are replaced and shared-Dong repository writes succeed")
    void migratesSoleSupportingUniquesAndPreservesSchema() throws Exception {
        TestSchema schema = createSchema();
        seed(schema);
        execute(schema, """
                ALTER TABLE tbl_member_detail
                    ADD UNIQUE INDEX uk_legacy_dong (dong_id),
                    ADD UNIQUE INDEX `uk``escaped` (dong_id),
                    ADD UNIQUE INDEX uk_place_description (place_id, description)
                """);
        dropOrdinaryDongIndexes(schema);

        assertThat(indexCount(schema, "NON_UNIQUE = 1 AND INDEX_TYPE = 'BTREE'"
                + " AND SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'dong_id' AND SUB_PART IS NULL")).isZero();
        assertThat(dongOnlyUniqueCount(schema)).isEqualTo(2);
        assertThat(queryLong(schema, "SELECT @@foreign_key_checks")).isOne();
        assertLegacyDuplicateFails(schema);
        printMetadata("BEFORE", schema);

        Flyway flyway = baselineAtSeven(schema);
        MigrateResult migrated = flyway.migrate();

        assertThat(migrated.migrationsExecuted).isEqualTo(1);
        assertThat(dongOnlyUniqueCount(schema)).isZero();
        assertThat(indexColumns(schema, "idx_member_detail_dong_id")).isEqualTo("dong_id");
        assertThat(indexCount(schema, "INDEX_NAME = 'idx_member_detail_dong_id'"
                + " AND NON_UNIQUE = 1 AND INDEX_TYPE = 'BTREE' AND SUB_PART IS NULL")).isOne();
        assertThat(indexColumns(schema, "uk_place_description")).isEqualTo("place_id,description");
        assertThat(indexColumns(schema, "PRIMARY")).isEqualTo("member_id");
        assertThat(queryString(schema, "SELECT IS_NULLABLE FROM information_schema.COLUMNS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tbl_member_detail'"
                + " AND COLUMN_NAME = 'dong_id'")).isEqualTo("NO");
        assertThat(queryLong(schema, "SELECT COUNT(*) FROM tbl_member_detail")).isOne();
        assertThat(queryLong(schema, "SELECT @@foreign_key_checks")).isOne();

        insertDetail(schema, 2, 1, 1, "shared");
        assertThat(queryLong(schema, "SELECT COUNT(*) FROM tbl_member_detail WHERE dong_id = 1")).isEqualTo(2);
        assertThatThrownBy(() -> insertDetail(schema, 4, 999999, 1, "dangling"))
                .isInstanceOfSatisfying(SQLException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(1452));
        assertThatThrownBy(() -> insertDetail(schema, 5, null, 1, "null-dong"))
                .isInstanceOfSatisfying(SQLException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(1048));
        assertThatThrownBy(() -> execute(schema, "INSERT INTO tbl_dong (dong_id, name) VALUES (2, 'seed-dong')"))
                .isInstanceOfSatisfying(SQLException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(1062));

        try (RepositoryContext context = repositoryContext(schema, "validate")) {
            MemberDetailRepository repository = context.context().getBean(MemberDetailRepository.class);
            PlatformTransactionManager transactions = context.context().getBean(PlatformTransactionManager.class);
            EntityManagerFactory entityManagerFactory = context.context().getBean(EntityManagerFactory.class);

            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                EntityManager entityManager = EntityManagerFactoryUtils
                        .getTransactionalEntityManager(entityManagerFactory);
                repository.saveAndFlush(MemberDetail.builder()
                        .member(entityManager.getReference(Member.class, 3L))
                        .dong(entityManager.getReference(Dong.class, 1))
                        .place(entityManager.getReference(Place.class, 1))
                        .gwangsan(0).light(0).description("repository")
                        .build());
            });
        }

        assertThat(queryLong(schema, "SELECT COUNT(*) FROM tbl_member_detail WHERE dong_id = 1")).isEqualTo(3);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        printMetadata("AFTER", schema);
    }

    @Test
    @DisplayName("already-fixed schema executes the actual migration as a no-op")
    void noOpsWhenDongUniqueIsAbsent() throws Exception {
        TestSchema schema = createSchema();
        seed(schema);
        String before = showCreate(schema);

        Flyway flyway = baselineAtSeven(schema);

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(showCreate(schema)).isEqualTo(before);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    @DisplayName("an existing full Dong-leading BTREE index is reused")
    void reusesExistingCompositeIndex() throws Exception {
        TestSchema schema = createSchema();
        seed(schema);
        execute(schema, "ALTER TABLE tbl_member_detail ADD UNIQUE INDEX arbitrary_dong_unique (dong_id)");
        execute(schema, "CREATE INDEX existing_dong_place ON tbl_member_detail (dong_id, place_id)");
        dropOrdinaryDongIndexesExcept(schema, "existing_dong_place");

        assertThat(indexCount(schema, "NON_UNIQUE = 1 AND INDEX_TYPE = 'BTREE'"
                + " AND SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'dong_id' AND SUB_PART IS NULL")).isOne();

        Flyway flyway = baselineAtSeven(schema);

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(dongOnlyUniqueCount(schema)).isZero();
        assertThat(indexColumns(schema, "existing_dong_place")).isEqualTo("dong_id,place_id");
        assertThat(indexColumns(schema, "idx_member_detail_dong_id")).isNull();
    }

    @Test
    @DisplayName("an incompatible replacement-index name fails before legacy uniqueness is dropped")
    void failsSafelyOnReplacementNameCollision() throws Exception {
        TestSchema schema = createSchema();
        seed(schema);
        execute(schema, "ALTER TABLE tbl_member_detail ADD UNIQUE INDEX keep_until_failure (dong_id)");
        dropOrdinaryDongIndexes(schema);
        execute(schema, "CREATE INDEX idx_member_detail_dong_id ON tbl_member_detail (place_id)");
        Flyway flyway = baselineAtSeven(schema);

        assertThatThrownBy(flyway::migrate)
                .rootCause()
                .hasMessageContaining("idx_member_detail_dong_id exists with an incompatible definition");

        assertThat(dongOnlyUniqueCount(schema)).isOne();
        assertThat(indexColumns(schema, "idx_member_detail_dong_id")).isEqualTo("place_id");
        assertThat(queryLong(schema, "SELECT COUNT(*) FROM tbl_member_detail")).isOne();
        assertThat(queryLong(schema, "SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS"
                + " WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'tbl_member_detail'")).isEqualTo(3);
        assertThatThrownBy(() -> insertDetail(schema, 4, 999999, 1, "still-protected"))
                .isInstanceOfSatisfying(SQLException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(1452));
    }

    private static TestSchema createSchema() throws SQLException {
        String name = "member_dong_v8_" + schemaSequence.incrementAndGet();
        try (Connection connection = adminConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + name + "` CHARACTER SET utf8mb4");
            statement.execute("GRANT ALL ON `" + name + "`.* TO '" + mariadb.getUsername() + "'@'%'");
        }
        schemas.add(name);
        TestSchema schema = new TestSchema(name, schemaUrl(name));
        System.out.printf("QA_RESOURCE_CREATED schema=%s%n", name);
        try (RepositoryContext ignored = repositoryContext(schema, "create")) {
        }
        return schema;
    }

    private static RepositoryContext repositoryContext(TestSchema schema, String ddlMode) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
                "migrationTest", Map.of("migration.jdbc-url", schema.url(), "migration.ddl-auto", ddlMode)));
        context.register(PersistenceConfiguration.class);
        context.refresh();
        return new RepositoryContext(context);
    }

    private static Flyway baselineAtSeven(TestSchema schema) {
        Flyway flyway = Flyway.configure()
                .dataSource(schema.url(), mariadb.getUsername(), mariadb.getPassword())
                .locations("classpath:db/migration")
                .baselineVersion("7")
                .load();
        flyway.baseline();
        return flyway;
    }

    private static void seed(TestSchema schema) throws SQLException {
        execute(schema, "INSERT INTO tbl_head_place (head_id, name) VALUES (1, 'seed-head')");
        execute(schema, "INSERT INTO tbl_place (place_id, name, head_id) VALUES (1, 'seed-place', 1)");
        execute(schema, "INSERT INTO tbl_dong (dong_id, name) VALUES (1, 'seed-dong')");
        for (long id = 1; id <= 5; id++) {
            execute(schema, "INSERT INTO tbl_member"
                    + " (member_id, name, nickname, password, phone_number, role, member_status, joined_at) VALUES ("
                    + id + ", 'member', 'nickname" + id + "', 'pw', '010-0000-000" + id
                    + "', 'ROLE_USER', 'ACTIVE', CURRENT_TIMESTAMP)");
        }
        insertDetail(schema, 1, 1, 1, "seed");
    }

    private static void assertLegacyDuplicateFails(TestSchema schema) {
        assertThatThrownBy(() -> insertDetail(schema, 2, 1, 1, "legacy-red"))
                .isInstanceOfSatisfying(SQLException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(1062);
                    System.out.printf("LEGACY_RED errorCode=%s duplicate_dong=%s%n",
                            error.getErrorCode(), error.getMessage());
                });
    }

    private static void insertDetail(TestSchema schema, long memberId, Integer dongId,
                                     int placeId, String description) throws SQLException {
        String dong = dongId == null ? "NULL" : dongId.toString();
        execute(schema, "INSERT INTO tbl_member_detail"
                + " (member_id, dong_id, place_id, gwangsan, light, description) VALUES ("
                + memberId + ", " + dong + ", " + placeId + ", 0, 0, '" + description + "')");
    }

    private static void dropOrdinaryDongIndexes(TestSchema schema) throws SQLException {
        dropOrdinaryDongIndexesExcept(schema, "");
    }

    private static void dropOrdinaryDongIndexesExcept(TestSchema schema, String retainedName) throws SQLException {
        try (Connection connection = connection(schema); Statement statement = connection.createStatement();
             ResultSet indexes = statement.executeQuery("""
                     SELECT DISTINCT INDEX_NAME
                     FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_NAME = 'tbl_member_detail'
                       AND NON_UNIQUE = 1
                       AND SEQ_IN_INDEX = 1
                       AND COLUMN_NAME = 'dong_id'
                     """)) {
            Set<String> names = new LinkedHashSet<>();
            while (indexes.next()) {
                names.add(indexes.getString(1));
            }
            for (String name : names) {
                if (name.equals(retainedName)) {
                    continue;
                }
                try (Statement drop = connection.createStatement()) {
                    drop.execute("ALTER TABLE tbl_member_detail DROP INDEX `" + name.replace("`", "``") + "`");
                }
            }
        }
    }

    private static long dongOnlyUniqueCount(TestSchema schema) throws SQLException {
        return queryLong(schema, """
                SELECT COUNT(*) FROM (
                    SELECT INDEX_NAME
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'tbl_member_detail'
                      AND NON_UNIQUE = 0
                      AND INDEX_NAME <> 'PRIMARY'
                    GROUP BY INDEX_NAME
                    HAVING COUNT(*) = 1 AND MIN(COLUMN_NAME) = 'dong_id'
                ) AS dong_unique_indexes
                """);
    }

    private static long indexCount(TestSchema schema, String condition) throws SQLException {
        return queryLong(schema, "SELECT COUNT(*) FROM information_schema.STATISTICS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tbl_member_detail' AND " + condition);
    }

    private static String indexColumns(TestSchema schema, String name) throws SQLException {
        try (Connection connection = connection(schema);
             var statement = connection.prepareStatement("""
                     SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
                     FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_NAME = 'tbl_member_detail'
                       AND INDEX_NAME = ?
                     """)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getString(1);
            }
        }
    }

    private static String showCreate(TestSchema schema) throws SQLException {
        try (Connection connection = connection(schema); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SHOW CREATE TABLE tbl_member_detail")) {
            result.next();
            return result.getString(2);
        }
    }

    private static void printMetadata(String phase, TestSchema schema) throws SQLException {
        System.out.printf("MIGRATION_%s_SCHEMA %s%n", phase, showCreate(schema));
        try (Connection connection = connection(schema); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SHOW INDEX FROM tbl_member_detail")) {
            while (result.next()) {
                System.out.printf("MIGRATION_%s_INDEX name=%s nonUnique=%s seq=%s column=%s type=%s%n",
                        phase, result.getString("Key_name"), result.getInt("Non_unique"),
                        result.getInt("Seq_in_index"), result.getString("Column_name"),
                        result.getString("Index_type"));
            }
        }
        try (Connection connection = connection(schema); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT CONSTRAINT_NAME, REFERENCED_TABLE_NAME
                     FROM information_schema.REFERENTIAL_CONSTRAINTS
                     WHERE CONSTRAINT_SCHEMA = DATABASE()
                       AND TABLE_NAME = 'tbl_member_detail'
                     ORDER BY CONSTRAINT_NAME
                     """)) {
            while (result.next()) {
                System.out.printf("MIGRATION_%s_FK name=%s referencedTable=%s%n",
                        phase, result.getString(1), result.getString(2));
            }
        }
    }

    private static long queryLong(TestSchema schema, String sql) throws SQLException {
        try (Connection connection = connection(schema); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static String queryString(TestSchema schema, String sql) throws SQLException {
        try (Connection connection = connection(schema); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private static void execute(TestSchema schema, String sql) throws SQLException {
        try (Connection connection = connection(schema); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static Connection adminConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(
                mariadb.getJdbcUrl(), "root", mariadb.getPassword());
    }

    private static Connection connection(TestSchema schema) throws SQLException {
        return java.sql.DriverManager.getConnection(
                schema.url(), mariadb.getUsername(), mariadb.getPassword());
    }

    private static String schemaUrl(String schema) {
        String database = "/" + mariadb.getDatabaseName();
        return mariadb.getJdbcUrl().replace(database, "/" + schema);
    }

    private record TestSchema(String name, String url) {
    }

    private record RepositoryContext(AnnotationConfigApplicationContext context) implements AutoCloseable {
        @Override
        public void close() {
            context.close();
        }
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaAuditing
    @EnableJpaRepositories(basePackageClasses = MemberDetailRepository.class)
    static class PersistenceConfiguration {

        @Bean
        DataSource dataSource(org.springframework.core.env.Environment environment) {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(mariadb.getDriverClassName());
            dataSource.setUrl(environment.getRequiredProperty("migration.jdbc-url"));
            dataSource.setUsername(mariadb.getUsername());
            dataSource.setPassword(mariadb.getPassword());
            return dataSource;
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(
                DataSource dataSource, org.springframework.core.env.Environment environment) {
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("team.startup.gwangsan.domain");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of(
                    "hibernate.hbm2ddl.auto", environment.getRequiredProperty("migration.ddl-auto"),
                    "hibernate.show_sql", "false"
            ));
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }

        @Bean
        JPAQueryFactory queryFactory(EntityManagerFactory entityManagerFactory) {
            return new JPAQueryFactory(SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory));
        }
    }
}
