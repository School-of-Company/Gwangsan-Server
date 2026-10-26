package team.startup.gwangsan.application.member.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.GwangsanBalancePersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.HeadJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;
import team.startup.gwangsan.application.member.data.BalanceMismatch;
import team.startup.gwangsan.application.member.port.in.ChangeGwangsanUseCase;
import team.startup.gwangsan.application.member.port.in.ReconcileGwangsanUseCase;
import team.startup.gwangsan.domain.member.model.GwangsanLedgerType;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 잔액 원자적 갱신과 원장 기록을 실제 MariaDB 에서 확인한다(유니크 키·행 잠금이 DB 동작이라 H2 를 쓰지 않는다). */
/** 대사는 원장 합과 잔액을 DB 에서 비교하므로 실제 MariaDB 로 확인한다. */
@DataJpaTest(properties = "spring.flyway.enabled=false")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, ChangeGwangsanService.class, ReconcileGwangsanService.class, GwangsanBalancePersistenceAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("ReconcileGwangsanService 통합 테스트")
class ReconcileGwangsanServiceIntegrationTest {

    @Container
    static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", mariadb::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @Autowired private ChangeGwangsanUseCase changeGwangsanUseCase;
    @Autowired private ReconcileGwangsanUseCase reconcileGwangsanUseCase;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    /** 대사는 전체 회원을 보므로 다른 테스트가 남긴 회원이 섞이지 않게 비운다. */
    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM tbl_gwangsan_ledger");
        jdbcTemplate.update("DELETE FROM tbl_member_detail");
    }

    @Nested
    @DisplayName("execute() 는")
    class Describe_execute {

        @Test
        @DisplayName("원장을 거치지 않고 잔액이 바뀐 회원만 돌려준다")
        void it_reports_member_whose_balance_bypassed_ledger() {
            long from = persistMember(0);
            long to = persistMember(0);
            tx(() -> {
                changeGwangsanUseCase.credit(from, 3_000, GwangsanLedgerType.SIGNUP_REWARD, from);
                changeGwangsanUseCase.transfer(from, to, 1_000, GwangsanLedgerType.TRADE_COMPLETE, 1L);
            });
            jdbcTemplate.update("UPDATE tbl_member_detail SET gwangsan = gwangsan + 1 WHERE member_id = ?", to);

            List<BalanceMismatch> mismatches = tx(() -> reconcileGwangsanUseCase.execute());

            assertThat(mismatches).containsExactly(new BalanceMismatch(to, 1_001, 1_000));
            assertThat(mismatches.get(0).balance() - mismatches.get(0).ledgerSum()).isEqualTo(1);
        }

        @Test
        @DisplayName("모든 잔액이 원장 합과 같으면 빈 목록을 돌려준다")
        void it_returns_empty_when_consistent() {
            long from = persistMember(0);
            long to = persistMember(0);
            tx(() -> {
                changeGwangsanUseCase.credit(from, 3_000, GwangsanLedgerType.SIGNUP_REWARD, from);
                changeGwangsanUseCase.transfer(from, to, 1_000, GwangsanLedgerType.TRADE_COMPLETE, 1L);
                changeGwangsanUseCase.adjust(to, -5_000);
            });

            assertThat(tx(() -> reconcileGwangsanUseCase.execute())).isEmpty();
        }
    }

    private <T> T tx(java.util.function.Supplier<T> body) {
        return new TransactionTemplate(transactionManager).execute(status -> body.get());
    }

    private void tx(Runnable body) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> body.run());
    }

    private int balanceOf(long memberId) {
        return jdbcTemplate.queryForObject(
                "SELECT gwangsan FROM tbl_member_detail WHERE member_id = ?", Integer.class, memberId);
    }

    private long persistMember(int gwangsan) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            HeadJpaEntity head = new HeadJpaEntity("본부" + suffix);
            em.persist(head);
            PlaceJpaEntity place = PlaceJpaEntity.builder().name("지점" + suffix).head(head).build();
            em.persist(place);
            DongJpaEntity dong = DongJpaEntity.builder().name("동" + suffix).build();
            em.persist(dong);
            MemberJpaEntity member = MemberJpaEntity.builder().name(suffix).nickname(suffix).phoneNumber(suffix)
                    .password("pw").role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
            em.persist(member);
            em.persist(MemberDetailJpaEntity.builder().member(member).dong(dong).place(place)
                    .gwangsan(gwangsan).light(0).description("소개").build());
            em.flush();
            return member.getId();
        });
    }
}
