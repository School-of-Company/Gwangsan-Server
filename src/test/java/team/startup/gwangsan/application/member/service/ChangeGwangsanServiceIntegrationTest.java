package team.startup.gwangsan.application.member.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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
import team.startup.gwangsan.application.member.port.in.ChangeGwangsanUseCase;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.model.GwangsanLedgerType;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 잔액 원자적 갱신과 원장 기록을 실제 MariaDB 에서 확인한다(유니크 키·행 잠금이 DB 동작이라 H2 를 쓰지 않는다). */
@DataJpaTest(properties = "spring.flyway.enabled=false")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, ChangeGwangsanService.class, GwangsanBalancePersistenceAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("ChangeGwangsanService 통합 테스트")
class ChangeGwangsanServiceIntegrationTest {

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
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    @Nested
    @DisplayName("transfer() 는")
    class Describe_transfer {

        @Test
        @DisplayName("잔액을 옮기고 원장 2행을 남긴다")
        void it_moves_balance_and_records_two_ledger_rows() {
            long from = persistMember(3_000);
            long to = persistMember(500);

            tx(() -> changeGwangsanUseCase.transfer(from, to, 1_000, GwangsanLedgerType.TRADE_COMPLETE, 11L));

            assertThat(balanceOf(from)).isEqualTo(2_000);
            assertThat(balanceOf(to)).isEqualTo(1_500);
            assertThat(ledgerOf(from)).containsExactly(row(-1_000, 2_000, "TRADE_COMPLETE", 11L));
            assertThat(ledgerOf(to)).containsExactly(row(1_000, 1_500, "TRADE_COMPLETE", 11L));
        }

        @Test
        @DisplayName("같은 (type, refId) 를 두 번 반영하면 트랜잭션 전체가 롤백된다")
        void it_rolls_back_whole_tx_on_duplicate_ref() {
            long from = persistMember(3_000);
            long to = persistMember(500);

            assertThatThrownBy(() -> tx(() -> {
                changeGwangsanUseCase.transfer(from, to, 1_000, GwangsanLedgerType.TRADE_COMPLETE, 77L);
                changeGwangsanUseCase.transfer(from, to, 1_000, GwangsanLedgerType.TRADE_COMPLETE, 77L);
            })).isInstanceOf(DataIntegrityViolationException.class);

            assertThat(balanceOf(from)).isEqualTo(3_000);
            assertThat(balanceOf(to)).isEqualTo(500);
            assertThat(ledgerOf(from)).isEmpty();
            assertThat(ledgerOf(to)).isEmpty();
        }

        @Test
        @DisplayName("잔액이 모자라도 음수로 옮긴다(기존 정책)")
        void it_allows_negative_balance_on_transfer() {
            long from = persistMember(100);
            long to = persistMember(0);

            tx(() -> changeGwangsanUseCase.transfer(from, to, 500, GwangsanLedgerType.TRADE_COMPLETE, 12L));

            assertThat(balanceOf(from)).isEqualTo(-400);
            assertThat(ledgerOf(from)).containsExactly(row(-500, -400, "TRADE_COMPLETE", 12L));
        }

        @Test
        @DisplayName("받는 회원의 상세가 없으면 NotFoundMemberDetailException 으로 보낸 쪽 차감까지 롤백된다")
        void it_rolls_back_when_receiver_detail_missing() {
            long from = persistMember(3_000);

            assertThatThrownBy(() -> tx(() ->
                    changeGwangsanUseCase.transfer(from, Long.MAX_VALUE, 1_000, GwangsanLedgerType.TRADE_COMPLETE, 13L)))
                    .isInstanceOf(NotFoundMemberDetailException.class);

            assertThat(balanceOf(from)).isEqualTo(3_000);
            assertThat(ledgerOf(from)).isEmpty();
        }
    }

    @Nested
    @DisplayName("credit() 는")
    class Describe_credit {

        @Test
        @DisplayName("상세가 없는 회원이면 NotFoundMemberDetailException 을 던진다")
        void it_throws_when_detail_missing() {
            assertThatThrownBy(() -> tx(() ->
                    changeGwangsanUseCase.credit(Long.MAX_VALUE, 5_000, GwangsanLedgerType.SIGNUP_REWARD, Long.MAX_VALUE)))
                    .isInstanceOf(NotFoundMemberDetailException.class);
        }
    }

    @Nested
    @DisplayName("adjust() 는")
    class Describe_adjust {

        @Test
        @DisplayName("잔액보다 큰 차감도 요청 값 그대로 반영해 음수가 된다")
        void it_applies_deduction_beyond_balance_as_requested() {
            long member = persistMember(3_000);

            tx(() -> changeGwangsanUseCase.adjust(member, -5_000));

            assertThat(balanceOf(member)).isEqualTo(-2_000);
            assertThat(ledgerOf(member)).containsExactly(row(-5_000, -2_000, "ADMIN_ADJUST", null));
        }

        @Test
        @DisplayName("음수 잔액 회원에게 지급·차감하면 요청 값만큼만 움직인다")
        void it_applies_adjustment_to_negative_balance_as_requested() {
            long member = persistMember(-2_000);

            tx(() -> changeGwangsanUseCase.adjust(member, 500));
            tx(() -> changeGwangsanUseCase.adjust(member, -500));

            assertThat(balanceOf(member)).isEqualTo(-2_000);
            assertThat(ledgerOf(member)).containsExactly(
                    row(500, -1_500, "ADMIN_ADJUST", null), row(-500, -2_000, "ADMIN_ADJUST", null));
        }

        @Test
        @DisplayName("조정은 refId 가 없어 몇 번이든 쌓인다")
        void it_records_every_adjustment() {
            long member = persistMember(1_000);

            tx(() -> changeGwangsanUseCase.adjust(member, 200));
            tx(() -> changeGwangsanUseCase.adjust(member, 300));

            assertThat(balanceOf(member)).isEqualTo(1_500);
            assertThat(ledgerOf(member)).containsExactly(
                    row(200, 1_200, "ADMIN_ADJUST", null), row(300, 1_500, "ADMIN_ADJUST", null));
        }
    }

    private void tx(Runnable body) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> body.run());
    }

    private int balanceOf(long memberId) {
        return jdbcTemplate.queryForObject(
                "SELECT gwangsan FROM tbl_member_detail WHERE member_id = ?", Integer.class, memberId);
    }

    private List<Map<String, Object>> ledgerOf(long memberId) {
        return jdbcTemplate.queryForList("SELECT amount, balance_after, type, ref_id FROM tbl_gwangsan_ledger "
                + "WHERE member_id = ? ORDER BY ledger_id", memberId);
    }

    private static Map<String, Object> row(int amount, int balanceAfter, String type, Long refId) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("amount", amount);
        row.put("balance_after", balanceAfter);
        row.put("type", type);
        row.put("ref_id", refId);
        return row;
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
