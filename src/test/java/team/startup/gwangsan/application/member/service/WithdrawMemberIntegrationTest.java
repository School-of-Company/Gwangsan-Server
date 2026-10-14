package team.startup.gwangsan.application.member.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.adapter.integration.member.MemberReferenceCleanupAdapter;
import team.startup.gwangsan.adapter.integration.member.MemberWithdrawalBridge;
import team.startup.gwangsan.adapter.persistence.admin.AdminAlertJpaEntity;
import team.startup.gwangsan.adapter.persistence.admin.AdminAlertPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.alert.AlertJpaEntity;
import team.startup.gwangsan.adapter.persistence.alert.AlertPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.alert.AlertReceiptJpaEntity;
import team.startup.gwangsan.adapter.persistence.block.MemberBlockJpaEntity;
import team.startup.gwangsan.adapter.persistence.block.MemberBlockJpaRepository;
import team.startup.gwangsan.adapter.persistence.chat.ChatMessageJpaEntity;
import team.startup.gwangsan.adapter.persistence.chat.ChatRoomJpaEntity;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaEntity;
import team.startup.gwangsan.adapter.persistence.notice.NoticeJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.adapter.persistence.post.ProductImagePersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.post.ProductPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.post.ProductReservation;
import team.startup.gwangsan.adapter.persistence.post.ReservationPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.relatedkeyword.MemberRelatedKeywordJpaEntity;
import team.startup.gwangsan.adapter.persistence.relatedkeyword.RelatedKeywordJpaEntity;
import team.startup.gwangsan.adapter.persistence.relatedkeyword.RelatedKeywordPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.report.ReportJpaEntity;
import team.startup.gwangsan.adapter.persistence.review.ReviewJpaEntity;
import team.startup.gwangsan.adapter.persistence.suspend.SuspendJpaEntity;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaEntity;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompleteJpaEntity;
import team.startup.gwangsan.application.admin.service.AdminAlertSupportService;
import team.startup.gwangsan.application.alert.service.AlertMemberCleanupService;
import team.startup.gwangsan.application.member.data.WithdrawMemberCommand;
import team.startup.gwangsan.application.member.port.in.WithdrawMemberUseCase;
import team.startup.gwangsan.application.post.service.PostReferenceCleanupService;
import team.startup.gwangsan.application.relatedkeyword.service.DeleteAllMemberRelatedKeywordsService;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.ReservationStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.report.model.ReportType;
import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.model.TradeStatus;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * MariaDB 로 회원 탈퇴의 다업무 재지정·삭제와 트랜잭션 rollback 을 확인한다.
 * 테스트 트랜잭션을 끄고 유스케이스가 직접 commit/rollback 하게 둔다.
 */
@DataJpaTest(properties = "spring.flyway.enabled=false")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, WithdrawMemberService.class, MemberWithdrawalBridge.class,
        MemberReferenceCleanupAdapter.class, AdminAlertSupportService.class, AdminAlertPersistenceAdapter.class,
        PostReferenceCleanupService.class, ProductPersistenceAdapter.class, ReservationPersistenceAdapter.class,
        ProductImagePersistenceAdapter.class, DeleteAllMemberRelatedKeywordsService.class,
        RelatedKeywordPersistenceAdapter.class, AlertMemberCleanupService.class, AlertPersistenceAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("MariaDB 회원 탈퇴 통합 테스트")
class WithdrawMemberIntegrationTest {

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

    @Autowired private WithdrawMemberUseCase withdrawMemberUseCase;
    @Autowired private EntityManager em;
    @Autowired private TransactionTemplate tx;
    @Autowired private JdbcTemplate jdbc;
    @MockitoSpyBean private MemberBlockJpaRepository memberBlockRepository;

    private record Ids(Long member, Long dummy, Long other, Long product, Long adminAlert, Long keyword,
                       Long reservation) {
    }

    @AfterEach
    void cleanUp() {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class);
        tables.forEach(table -> jdbc.execute("TRUNCATE TABLE " + table));
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    @Nested
    @DisplayName("정상 탈퇴는")
    class Describe_success {

        @Test
        @DisplayName("it_관심 키워드가 있어도 탈퇴 전용 bulk 삭제로 키워드를 지우고 탈퇴한다 (키워드 원본은 유지)")
        void it_withdraws_member_with_keyword() {
            Ids ids = tx.execute(status -> givenMember(false, true, false));

            withdrawMemberUseCase.execute(new WithdrawMemberCommand(ids.member(), false));

            assertWithdrawn(ids, false);
            assertThat(sql("SELECT COUNT(*) FROM tbl_member_related_keyword WHERE member_id = ?", ids.member())).isZero();
            assertThat(sql("SELECT COUNT(*) FROM tbl_related_keyword WHERE keyword_id = ?", ids.keyword())).isOne();
        }

        @Test
        @DisplayName("it_예약자인 회원은 예약 reserver 를 dummy 로 재지정하고 탈퇴한다")
        void it_withdraws_reserver() {
            Ids ids = tx.execute(status -> givenMember(false, false, true));

            withdrawMemberUseCase.execute(new WithdrawMemberCommand(ids.member(), false));

            assertWithdrawn(ids, false);
            assertThat(sql("SELECT reserver_id FROM tbl_product_reservation WHERE product_reservation_id = ?",
                    ids.reservation())).isEqualTo(ids.dummy());
        }

        @Test
        @DisplayName("it_키워드·예약을 포함한 모든 업무 참조를 가진 회원도 전부 정리하고 탈퇴한다")
        void it_withdraws_member_with_every_reference() {
            Ids ids = tx.execute(status -> givenMember(true, true, true));

            withdrawMemberUseCase.execute(new WithdrawMemberCommand(ids.member(), true));

            assertWithdrawn(ids, true);
            tx.executeWithoutResult(status -> {
                Long dummy = ids.dummy();
                assertThat(em.find(MemberJpaEntity.class, ids.other()).getRecommender().getId()).isEqualTo(dummy);
                assertThat(em.find(Product.class, ids.product()).getMember().getId()).isEqualTo(dummy);
                assertThat(em.find(ProductReservation.class, ids.reservation()).getReserver().getId()).isEqualTo(dummy);
                assertThat(singleId("SELECT t.buyer.id FROM TradeCompleteJpaEntity t")).isEqualTo(dummy);
                assertThat(singleId("SELECT c.seller.id FROM ChatRoomJpaEntity c")).isEqualTo(dummy);
                assertThat(singleId("SELECT m.sender.id FROM ChatMessageJpaEntity m")).isEqualTo(dummy);
                assertThat(singleId("SELECT r.reviewer.id FROM ReviewJpaEntity r")).isEqualTo(dummy);
                assertThat(singleId("SELECT r.reported.id FROM ReportJpaEntity r")).isEqualTo(dummy);
                assertThat(singleId("SELECT r.reporter.id FROM ReportJpaEntity r")).isEqualTo(dummy);
                AdminAlertJpaEntity alert = em.find(AdminAlertJpaEntity.class, ids.adminAlert());
                assertThat(alert.getRequester().getId()).isEqualTo(dummy);
                assertThat(alert.getOtherMember().getId()).isEqualTo(dummy);
                assertThat(singleId("SELECT t.member.id FROM TradeCancelJpaEntity t")).isEqualTo(dummy);
                assertThat(singleId("SELECT a.sendMember.id FROM AlertJpaEntity a")).isEqualTo(dummy);
                assertThat(singleId("SELECT n.member.id FROM NoticeJpaEntity n")).isEqualTo(dummy);
            });
            assertThat(sql("SELECT COUNT(*) FROM tbl_member_related_keyword WHERE member_id = ?", ids.member())).isZero();
            assertThat(sql("SELECT COUNT(*) FROM tbl_suspend WHERE member_id = ?", ids.member())).isZero();
            assertThat(sql("SELECT COUNT(*) FROM tbl_member_block WHERE blocker_id = ? OR blocked_id = ?",
                    ids.member(), ids.member())).isZero();
            assertThat(sql("SELECT COUNT(*) FROM tbl_alert_receipt WHERE member_id = ?", ids.member())).isZero();
        }
    }

    @Nested
    @DisplayName("중간에 실패하면")
    class Describe_failure {

        @Test
        @DisplayName("it_키워드 bulk 삭제·예약 재지정·정지 삭제 이후 단계 실패에도 탈퇴 기록·재지정·삭제를 전부 rollback 한다")
        void it_rolls_back_everything() {
            Ids ids = tx.execute(status -> givenMember(true, true, true));
            // 차단 삭제는 예약자 재지정·키워드 bulk 삭제·정지 삭제 뒤, 알림 수신 기록 삭제 앞 단계다.
            doThrow(new IllegalStateException("주입한 중간 실패")).when(memberBlockRepository).deleteAllByBlockerOrBlocked(any());

            assertThatThrownBy(() -> withdrawMemberUseCase.execute(new WithdrawMemberCommand(ids.member(), true)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("주입한 중간 실패");

            assertThat(sql("SELECT COUNT(*) FROM tbl_member WHERE member_id = ?", ids.member())).isOne();
            assertThat(sql("SELECT COUNT(*) FROM tbl_member_detail WHERE member_id = ?", ids.member())).isOne();
            assertThat(sql("SELECT COUNT(*) FROM tbl_withdrawal_record")).isZero();
            assertThat(sql("SELECT member_id FROM tbl_product WHERE product_id = ?", ids.product())).isEqualTo(ids.member());
            assertThat(sql("SELECT reserver_id FROM tbl_product_reservation WHERE product_reservation_id = ?",
                    ids.reservation())).isEqualTo(ids.member());
            assertThat(sql("SELECT requester_id FROM tbl_admin_alert WHERE admin_alert_id = ?", ids.adminAlert()))
                    .isEqualTo(ids.member());
            assertThat(sql("SELECT recommender_id FROM tbl_member WHERE member_id = ?", ids.other())).isEqualTo(ids.member());
            assertThat(sql("SELECT COUNT(*) FROM tbl_member_related_keyword WHERE member_id = ?", ids.member())).isOne();
            assertThat(sql("SELECT COUNT(*) FROM tbl_suspend WHERE member_id = ?", ids.member())).isOne();
            assertThat(sql("SELECT COUNT(*) FROM tbl_member_block WHERE blocked_id = ?", ids.member())).isOne();
            assertThat(sql("SELECT COUNT(*) FROM tbl_alert_receipt WHERE member_id = ?", ids.member())).isOne();
        }
    }

    private void assertWithdrawn(Ids ids, boolean banned) {
        assertThat(sql("SELECT COUNT(*) FROM tbl_member WHERE member_id = ?", ids.member())).isZero();
        assertThat(sql("SELECT COUNT(*) FROM tbl_member_detail WHERE member_id = ?", ids.member())).isZero();
        tx.executeWithoutResult(status -> {
            WithdrawalRecordJpaEntity record = em.createQuery("SELECT w FROM WithdrawalRecord w", WithdrawalRecordJpaEntity.class)
                    .getSingleResult();
            assertThat(record.getPhoneNumber()).isEqualTo("01000000001");
            assertThat(record.getGwangsan()).isEqualTo(7300);
            assertThat(record.isBanned()).isEqualTo(banned);
        });
    }

    private Ids givenMember(boolean everyReference, boolean withKeyword, boolean withReservation) {
        DongJpaEntity dong = persist(DongJpaEntity.builder().name("동").build());
        PlaceJpaEntity place = persist(PlaceJpaEntity.builder().name("지점").build());
        MemberJpaEntity dummy = persistMember("__withdrawn__", "01099999999", null);
        MemberJpaEntity member = persistMember("탈퇴자", "01000000001", null);
        MemberJpaEntity other = persistMember("상대", "01000000002", member);
        persist(MemberDetailJpaEntity.builder().member(member).dong(dong).place(place).gwangsan(7300).light(10).description("").build());
        persist(MemberDetailJpaEntity.builder().member(dummy).dong(dong).place(place).gwangsan(0).light(10).description("").build());
        Product product = persist(Product.builder().member(member).title("상품").description("설명").gwangsan(100)
                .type(Type.OBJECT).mode(Mode.GIVER).status(ProductStatus.ONGOING).build());

        Long adminAlertId = null;
        if (everyReference) {
            TradeCompleteJpaEntity trade = persist(TradeCompleteJpaEntity.builder().product(product).buyer(member).seller(other)
                    .status(TradeStatus.COMPLETED).requestedBySeller(false).build());
            ChatRoomJpaEntity room = persist(ChatRoomJpaEntity.builder().createdAt(LocalDateTime.now()).isActive(true).buyer(other)
                    .seller(member).product(product).build());
            persist(ChatMessageJpaEntity.builder().id(1L).content("안녕").messageType(MessageType.TEXT).checked(false)
                    .room(room).sender(member).createdAt(LocalDateTime.now()).build());
            persist(ReviewJpaEntity.builder().content("후기").light(5).reviewer(member).reviewed(other).product(product).build());
            persist(ReportJpaEntity.builder().reportType(ReportType.ETC).content("신고").reported(member).reporter(member).build());
            adminAlertId = persist(AdminAlertJpaEntity.builder().type(AlertType.REPORT).title("신고")
                    .sourceId(1L).requester(member).otherMember(member).build()).getId();
            persist(TradeCancelJpaEntity.builder().tradeComplete(trade).member(member).reason("사유")
                    .status(TradeCancelStatus.PENDING).build());
            AlertJpaEntity alert = persist(AlertJpaEntity.builder().sourceId(1L).sendMember(member)
                    .alertType(team.startup.gwangsan.domain.alert.model.AlertType.NOTICE)
                    .title("알림").content("내용").build());
            persist(NoticeJpaEntity.builder().title("공지").content("내용").place(place).member(member).build());
            LocalDateTime now = LocalDateTime.now();
            persist(SuspendJpaEntity.builder().suspendedAt(now).suspendedDays(1).suspendedUntil(now.plusDays(1)).member(member).build());
            persist(MemberBlockJpaEntity.builder().blocker(other).blocked(member).build());
            persist(AlertReceiptJpaEntity.builder().alert(alert).member(member).checked(false).build());
        }
        Long keywordId = null;
        if (withKeyword) {
            RelatedKeywordJpaEntity keyword = persist(RelatedKeywordJpaEntity.builder().name("키워드").build());
            persist(MemberRelatedKeywordJpaEntity.builder().relatedKeyword(keyword).member(member).build());
            keywordId = keyword.getId();
        }
        Long reservationId = null;
        if (withReservation) {
            reservationId = persist(ProductReservation.builder().product(product).reserver(member)
                    .status(ReservationStatus.PENDING).build()).getId();
        }
        return new Ids(member.getId(), dummy.getId(), other.getId(), product.getId(), adminAlertId, keywordId, reservationId);
    }

    private MemberJpaEntity persistMember(String nickname, String phone, MemberJpaEntity recommender) {
        return persist(MemberJpaEntity.builder().name(nickname).nickname(nickname).phoneNumber(phone).password("pw")
                .recommender(recommender).role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build());
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    private Long sql(String query, Object... args) {
        return jdbc.queryForObject(query, Long.class, args);
    }

    private Long singleId(String jpql) {
        return em.createQuery(jpql, Long.class).getSingleResult();
    }
}
