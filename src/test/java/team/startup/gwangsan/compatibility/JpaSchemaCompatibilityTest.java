package team.startup.gwangsan.compatibility;

import jakarta.persistence.Entity;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 헥사고날 전환 중 JPA 클래스 이동/이름 변경이 물리 스키마를 바꾸지 않는지 고정한다.
 * 운영은 ddl-auto=validate 이고 테스트는 create 라, 테이블명이 바뀌어도 다른 테스트는 통과한다.
 * 스냅샷은 클래스 이름이 아니라 테이블 기준이므로 Entity → XxxJpaEntity 이동은 통과하고
 * 테이블/컬럼/타입/enum 값/nullable/PK/FK/unique/index 변경만 실패한다.
 * Spring Boot 기본 naming 전략과 MariaDB dialect 로 DB 연결 없이 Hibernate 메타데이터를 만든다.
 */
@DisplayName("JPA 물리 스키마 호환")
class JpaSchemaCompatibilityTest {

    private static final String ROOT = "team.startup.gwangsan";
    private static final String SNAPSHOT = "compatibility/jpa-schema.snapshot";

    private static StandardServiceRegistry registry;
    private static Metadata metadata;
    private static List<Class<?>> entities;

    @BeforeAll
    static void buildMetadata() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        entities = new ArrayList<>();
        for (var candidate : scanner.findCandidateComponents(ROOT)) {
            entities.add(Class.forName(candidate.getBeanClassName()));
        }

        registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.MariaDBDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY, CamelCaseToUnderscoresNamingStrategy.class.getName())
                .applySetting(AvailableSettings.IMPLICIT_NAMING_STRATEGY, SpringImplicitNamingStrategy.class.getName())
                .build();
        MetadataSources sources = new MetadataSources(registry);
        entities.forEach(sources::addAnnotatedClass);
        metadata = sources.buildMetadata();
    }

    @AfterAll
    static void close() {
        StandardServiceRegistryBuilder.destroy(registry);
    }

    @Nested
    @DisplayName("현재 엔티티 매핑은")
    class Describe_current_mapping {

        @Test
        @DisplayName("it_검사 대상 엔티티를 실제로 찾는다 (빈 대상 통과 방지)")
        void it_scans_entities() {
            assertThat(entities).hasSizeGreaterThanOrEqualTo(29);
            assertThat(metadata.getEntityBindings()).hasSize(entities.size());
        }

        @Test
        @DisplayName("it_기준 스키마 스냅샷과 같다")
        void it_matches_baseline_snapshot() {
            Snapshots.assertMatches(SNAPSHOT, render());
        }
    }

    private static String render() {
        StringBuilder out = new StringBuilder();
        metadata.getDatabase().getDefaultNamespace().getTables().stream()
                .sorted(Comparator.comparing(Table::getName))
                .forEach(table -> renderTable(out, table));
        out.append("\n# entity -> table (클래스 이름은 비교하지 않고 테이블 존재만 확인)\n");
        metadata.getEntityBindings().stream()
                .map(PersistentClass::getTable)
                .map(Table::getName)
                .collect(Collectors.toCollection(TreeSet::new))
                .forEach(name -> out.append("mapped ").append(name).append('\n'));
        return out.toString();
    }

    private static void renderTable(StringBuilder out, Table table) {
        out.append("table ").append(table.getName()).append('\n');
        for (Column column : table.getColumns().stream().sorted(Comparator.comparing(Column::getName)).toList()) {
            out.append("  column ").append(column.getName())
                    .append(' ').append(column.getSqlType(metadata))
                    .append(column.isNullable() ? " null" : " not-null")
                    .append(column.isUnique() ? " unique" : "")
                    .append(column.isIdentity() ? " identity" : "")
                    .append('\n');
        }
        if (table.getPrimaryKey() != null) {
            out.append("  pk ").append(columns(table.getPrimaryKey().getColumns())).append('\n');
        }
        table.getForeignKeys().values().stream()
                .map(JpaSchemaCompatibilityTest::foreignKey)
                .sorted()
                .forEach(fk -> out.append("  fk ").append(fk).append('\n'));
        table.getUniqueKeys().values().stream()
                .map(UniqueKey::getColumns)
                .map(JpaSchemaCompatibilityTest::columns)
                .sorted()
                .forEach(uk -> out.append("  unique ").append(uk).append('\n'));
        table.getIndexes().values().stream()
                .map(JpaSchemaCompatibilityTest::index)
                .sorted()
                .forEach(idx -> out.append("  index ").append(idx).append('\n'));
    }

    private static String foreignKey(ForeignKey fk) {
        return columns(fk.getColumns()) + " -> " + fk.getReferencedTable().getName();
    }

    private static String index(Index index) {
        return index.getName() + " " + index.getSelectables().stream()
                .map(s -> s.getText())
                .collect(Collectors.joining(",", "(", ")"));
    }

    private static String columns(List<Column> columns) {
        return columns.stream().map(Column::getName).collect(Collectors.joining(",", "(", ")"));
    }
}
