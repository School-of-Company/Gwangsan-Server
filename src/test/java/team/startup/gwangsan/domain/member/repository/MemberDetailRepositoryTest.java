package team.startup.gwangsan.domain.member.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(QueryDslConfig.class)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("MemberDetail Dong 관계 통합 테스트")
class MemberDetailRepositoryTest {

    @Container
    static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", mariadb::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired private EntityManager entityManager;
    @Autowired private MemberDetailRepository memberDetailRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("같은 동을 사용하는 두 회원 상세 정보를 저장하고 다시 읽는다")
    void savesAndReloadsTwoDetailsWithOneDong() {
        inTransaction(() -> {
            Place place = place("공유");
            Dong dong = persist(Dong.builder().name("공유동").build());
            Member first = member("첫회원", "010-0000-0001");
            Member second = member("둘회원", "010-0000-0002");

            memberDetailRepository.save(MemberDetail.builder()
                    .member(first).dong(dong).place(place).gwangsan(0).light(0).description("소개")
                    .build());
            memberDetailRepository.save(MemberDetail.builder()
                    .member(second).dong(dong).place(place).gwangsan(0).light(0).description("소개")
                    .build());
            entityManager.flush();
            entityManager.clear();

            List<MemberDetail> details = memberDetailRepository.findAllByMemberIdIn(List.of(first.getId(), second.getId()));

            assertThat(details).hasSize(2);
            assertThat(details).extracting(detail -> detail.getDong().getId())
                    .containsOnly(dong.getId());
        });
    }

    @Test
    @DisplayName("서로 다른 동을 사용하는 회원 상세 정보도 저장한다")
    void savesDetailsWithDistinctDongs() {
        inTransaction(() -> {
            Place place = place("서로다름");
            Member first = member("셋회원", "010-0000-0003");
            Member second = member("넷회원", "010-0000-0004");

            memberDetailRepository.save(MemberDetail.builder()
                    .member(first).dong(persist(Dong.builder().name("첫동").build())).place(place)
                    .gwangsan(0).light(0).description("소개").build());
            memberDetailRepository.save(MemberDetail.builder()
                    .member(second).dong(persist(Dong.builder().name("둘동").build())).place(place)
                    .gwangsan(0).light(0).description("소개").build());

            entityManager.flush();
            entityManager.clear();

            assertThat(memberDetailRepository.findAllByMemberIdIn(List.of(first.getId(), second.getId()))).hasSize(2);
        });
    }

    @Test
    @DisplayName("fresh schema는 member_id 기본 키와 Dong FK 및 NOT NULL을 유지하고 Dong 단독 유니크를 만들지 않는다")
    void retainsRequiredConstraintsWithoutDongOnlyUnique() {
        ConstraintFixture fixture = inTransaction(() -> {
            Place place = place("제약");
            Member danglingMember = member("다섯회원", "010-0000-0005");
            Member nullDongMember = member("여섯회원", "010-0000-0006");
            entityManager.flush();
            return new ConstraintFixture(place.getId(), danglingMember.getId(), nullDongMember.getId());
        });

        assertThatThrownBy(() -> inTransaction(() -> {
            entityManager.createNativeQuery("""
                    INSERT INTO tbl_member_detail (member_id, description, gwangsan, light, dong_id, place_id)
                    VALUES (?, '소개', 0, 0, 999999, ?)
                    """)
                    .setParameter(1, fixture.danglingMemberId())
                    .setParameter(2, fixture.placeId())
                    .executeUpdate();
        })).hasMessageContaining("foreign key constraint fails");

        assertThatThrownBy(() -> inTransaction(() -> {
            entityManager.createNativeQuery("""
                    INSERT INTO tbl_member_detail (member_id, description, gwangsan, light, dong_id, place_id)
                    VALUES (?, '소개', 0, 0, NULL, ?)
                    """)
                    .setParameter(1, fixture.nullDongMemberId())
                    .setParameter(2, fixture.placeId())
                    .executeUpdate();
        })).hasMessageContaining("cannot be null");

        assertThat(inTransaction(() -> singleIndexColumns("PRIMARY"))).isEqualTo("member_id");
        assertThat(inTransaction(this::dongOnlyUniqueIndexCount)).isZero();
    }

    private void inTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private <T> T inTransaction(Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    private Place place(String suffix) {
        Head head = persist(new Head("본부-" + suffix));
        return persist(Place.builder().name("지점-" + suffix).head(head).build());
    }

    private Member member(String name, String phoneNumber) {
        return persist(Member.builder()
                .name(name).nickname(name).password("pw").phoneNumber(phoneNumber)
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE)
                .build());
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private String singleIndexColumns(String indexName) {
        return (String) entityManager.createNativeQuery("""
                SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'tbl_member_detail'
                  AND INDEX_NAME = ?
                """)
                .setParameter(1, indexName)
                .getSingleResult();
    }

    private long dongOnlyUniqueIndexCount() {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM (
                    SELECT INDEX_NAME
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'tbl_member_detail'
                      AND NON_UNIQUE = 0
                      AND INDEX_NAME <> 'PRIMARY'
                    GROUP BY INDEX_NAME
                    HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'dong_id'
                ) AS dong_unique_indexes
                """).getSingleResult()).longValue();
    }

    private record ConstraintFixture(Integer placeId, Long danglingMemberId, Long nullDongMemberId) {
    }
}
