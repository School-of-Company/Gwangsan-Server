package team.startup.gwangsan.application.trade.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.adapter.event.trade.TradeStatusChangedEvent;
import team.startup.gwangsan.adapter.event.notification.PushRequestedEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompleteJpaEntity;
import team.startup.gwangsan.application.trade.TradeIntegrationWiring;
import team.startup.gwangsan.application.trade.data.RequestTradeCancelCommand;
import team.startup.gwangsan.application.trade.data.TradeCompletionCommand;
import team.startup.gwangsan.domain.alert.model.AlertType;
import team.startup.gwangsan.application.alert.service.CreateAlertService;
import team.startup.gwangsan.application.alert.data.RecordAlertCommand;
import team.startup.gwangsan.adapter.persistence.chat.ChatMessageJpaEntity;
import team.startup.gwangsan.adapter.persistence.chat.ChatRoomJpaEntity;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.adapter.persistence.notification.DeviceTokenRepository;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.model.TradeStatus;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

/**
 * 중요한 거래 알림이 거래·잔액 변경과 같은 DB 트랜잭션에 저장되는지 실제 MariaDB 로 검증한다.
 * 알림 저장이 실패하면 거래·잔액·상품·철회 상태가 모두 rollback 돼야 한다.
 */
@DataJpaTest(properties = "spring.flyway.enabled=false")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, TradeIntegrationWiring.class, TradeAlertAtomicityIntegrationTest.AfterCommitRecorder.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("중요 거래 알림 원자 저장 통합 테스트")
class TradeAlertAtomicityIntegrationTest {

    private static final int GWANGSAN = 700;
    private static final int BALANCE = 10_000;
    private static final AtomicLong MESSAGE_IDS = new AtomicLong(1_000);

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

    @MockitoBean private DeviceTokenRepository deviceTokenRepository;
    @Autowired private AfterCommitRecorder afterCommit;
    @MockitoSpyBean private CreateAlertService createAlertService;

    @Autowired private RequestTradeCompletionService requestTradeCompletionService;
    @Autowired private RequestTradeCancelService requestTradeCancelService;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    @AfterEach
    void resetSpy() {
        reset(createAlertService);
    }

    @Nested
    @DisplayName("거래 완료 요청은")
    class Describe_completion_request {

        @Test
        @DisplayName("비작성자(구매자)의 요청은 PENDING 거래와 작성자 알림·receipt 를 같은 트랜잭션에서 커밋한다")
        void it_commits_trade_and_alert_together() {
            Fixture f = fixture(null);

            requestTradeCompletionService.execute(new TradeCompletionCommand(f.buyerId, f.productId, f.sellerId));

            assertThat(countTrades(f.productId, TradeStatus.PENDING)).isEqualTo(1);
            // 완료 요청 알림의 sourceId 는 상품 ID 다(기존 동작과 같음).
            assertThat(alertCount(f.productId, AlertType.OTHER_MEMBER_TRADE_COMPLETE)).isEqualTo(1);
            assertThat(receiptMembers(f.productId, AlertType.OTHER_MEMBER_TRADE_COMPLETE)).containsExactly(f.sellerId);
            assertThat(afterCommit.statusChanges(f.productId)).containsExactly(false);
            assertThat(afterCommit.pushes(f.productId)).isZero();
        }

        @Test
        @DisplayName("알림 저장이 실패하면 PENDING 거래도 남기지 않는다")
        void it_rolls_back_trade_when_alert_fails() {
            Fixture f = fixture(null);
            doThrow(new IllegalStateException("alert down")).when(createAlertService)
                    .record(argThat((RecordAlertCommand c) -> c.alert().type() == AlertType.OTHER_MEMBER_TRADE_COMPLETE));
            int alertsBefore = totalAlerts();

            assertThatThrownBy(() -> requestTradeCompletionService.execute(
                    new TradeCompletionCommand(f.buyerId, f.productId, f.sellerId)))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(countTrades(f.productId, TradeStatus.PENDING)).isZero();
            assertThat(totalAlerts()).isEqualTo(alertsBefore);
            // rollback 되면 커밋 후 전달(거래 상태)도 일어나지 않는다.
            assertThat(afterCommit.statusChanges(f.productId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("게시글 작성자의 즉시 완료는")
    class Describe_author_completion {

        @Test
        @DisplayName("COMPLETED 거래·상품 완료·잔액 1회 이동을 커밋하고, 알림함 기록 없이 커밋 후 푸시와 상태 전달만 한다")
        void it_completes_and_pushes_after_commit() {
            Fixture f = fixture(null);
            int alertsBefore = totalAlerts();

            requestTradeCompletionService.execute(new TradeCompletionCommand(f.sellerId, f.productId, f.buyerId));

            assertThat(countTrades(f.productId, TradeStatus.COMPLETED)).isEqualTo(1);
            assertThat(countTrades(f.productId, TradeStatus.PENDING)).isZero();
            assertThat(productStatus(f.productId)).isEqualTo(ProductStatus.COMPLETED.name());
            assertThat(jdbc.queryForObject("SELECT completed_at IS NOT NULL FROM tbl_trade_complete WHERE product_id = ?",
                    Boolean.class, f.productId)).isTrue();
            assertThat(gwangsanOf(f.buyerId)).isEqualTo(BALANCE - GWANGSAN);
            assertThat(gwangsanOf(f.sellerId)).isEqualTo(BALANCE + GWANGSAN);
            // TRADE_COMPLETE 알림함 기록은 신규로 만들지 않는다(확정 정책).
            assertThat(totalAlerts()).isEqualTo(alertsBefore);
            assertThat(afterCommit.pushes(f.productId)).isEqualTo(1);
            assertThat(afterCommit.statusChanges(f.productId)).containsExactly(true);
        }
    }

    @Nested
    @DisplayName("양측 동의 철회는")
    class Describe_cancel_agreement {

        @Test
        @DisplayName("환불·상태 전이와 철회 완료 알림(양쪽 receipt)을 함께 커밋한다")
        void it_commits_cancel_and_alert_together() {
            Fixture f = fixture(true);
            long cancelId = requestCancel(f, f.sellerId);

            assertThat(requestTradeCancelService.execute(
                    new RequestTradeCancelCommand(f.buyerId, f.productId, "동의", List.of())).cancelled()).isTrue();

            assertThat(gwangsanOf(f.buyerId)).isEqualTo(BALANCE + GWANGSAN);
            assertThat(gwangsanOf(f.sellerId)).isEqualTo(BALANCE - GWANGSAN);
            assertThat(cancelStatus(cancelId)).isEqualTo(TradeCancelStatus.APPROVED.name());
            assertThat(countTrades(f.productId, TradeStatus.ROLLED_BACK)).isEqualTo(1);
            assertThat(productStatus(f.productId)).isEqualTo(ProductStatus.ONGOING.name());
            assertThat(receiptMembers(cancelId, AlertType.TRADE_CANCEL)).containsExactlyInAnyOrder(f.buyerId, f.sellerId);
        }

        @Test
        @DisplayName("철회 완료 알림 저장이 실패하면 잔액·거래·상품·철회 상태를 모두 되돌린다")
        void it_rolls_back_everything_when_alert_fails() {
            Fixture f = fixture(true);
            long cancelId = requestCancel(f, f.sellerId);
            doThrow(new IllegalStateException("alert down")).when(createAlertService)
                    .record(argThat((RecordAlertCommand c) -> c.alert().type() == AlertType.TRADE_CANCEL));

            assertThatThrownBy(() -> requestTradeCancelService.execute(
                    new RequestTradeCancelCommand(f.buyerId, f.productId, "동의", List.of())))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(gwangsanOf(f.buyerId)).isEqualTo(BALANCE);
            assertThat(gwangsanOf(f.sellerId)).isEqualTo(BALANCE);
            assertThat(cancelStatus(cancelId)).isEqualTo(TradeCancelStatus.PENDING.name());
            assertThat(countTrades(f.productId, TradeStatus.COMPLETED)).isEqualTo(1);
            assertThat(productStatus(f.productId)).isEqualTo(ProductStatus.COMPLETED.name());
            assertThat(alertCount(cancelId, AlertType.TRADE_CANCEL)).isZero();
        }
    }

    private long requestCancel(Fixture f, long requesterId) {
        assertThat(requestTradeCancelService.execute(
                new RequestTradeCancelCommand(requesterId, f.productId, "사유", List.of())).cancelled()).isFalse();
        return jdbc.queryForObject("SELECT trade_cancel_id FROM tbl_trade_cancel WHERE status = 'PENDING' AND member_id = ?",
                Long.class, requesterId);
    }

    private int totalAlerts() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_alert", Integer.class);
    }

    private int alertCount(long sourceId, AlertType type) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_alert WHERE source_id = ? AND alert_type = ?",
                Integer.class, sourceId, type.name());
    }

    private List<Long> receiptMembers(long sourceId, AlertType type) {
        return jdbc.queryForList("SELECT r.member_id FROM tbl_alert_receipt r JOIN tbl_alert a ON a.alert_id = r.alert_id "
                + "WHERE a.source_id = ? AND a.alert_type = ?", Long.class, sourceId, type.name());
    }

    private int countTrades(long productId, TradeStatus status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_trade_complete WHERE product_id = ? AND status = ?",
                Integer.class, productId, status.name());
    }

    private int gwangsanOf(long memberId) {
        return jdbc.queryForObject("SELECT gwangsan FROM tbl_member_detail WHERE member_id = ?", Integer.class, memberId);
    }

    private String cancelStatus(long cancelId) {
        return jdbc.queryForObject("SELECT status FROM tbl_trade_cancel WHERE trade_cancel_id = ?", String.class, cancelId);
    }

    private String productStatus(long productId) {
        return jdbc.queryForObject("SELECT status FROM tbl_product WHERE product_id = ?", String.class, productId);
    }

    /** completed 가 null 이면 채팅이 오간 진행 중 상품, true 면 완료된 거래까지 저장한다. */
    private Fixture fixture(Boolean completed) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Head head = new Head("본부" + suffix);
            em.persist(head);
            Place place = Place.builder().name("지점" + suffix).head(head).build();
            em.persist(place);
            DongJpaEntity dong = DongJpaEntity.builder().name("동" + suffix).build();
            em.persist(dong);

            MemberJpaEntity seller = member("seller" + suffix);
            MemberJpaEntity buyer = member("buyer" + suffix);
            em.persist(seller);
            em.persist(buyer);
            em.persist(MemberDetailJpaEntity.builder().member(seller).dong(dong).gwangsan(BALANCE).place(place).light(0).description("소개").build());
            em.persist(MemberDetailJpaEntity.builder().member(buyer).dong(dong).gwangsan(BALANCE).place(place).light(0).description("소개").build());

            Product product = Product.builder().title("상품").description("설명").gwangsan(GWANGSAN).member(seller)
                    .type(Type.OBJECT).mode(Mode.GIVER)
                    .status(completed == null ? ProductStatus.ONGOING : ProductStatus.COMPLETED).build();
            em.persist(product);

            ChatRoomJpaEntity room = ChatRoomJpaEntity.builder().product(product).buyer(buyer).seller(seller).isActive(true).build();
            em.persist(room);
            em.persist(message(room, seller));
            em.persist(message(room, buyer));

            if (completed != null) {
                em.persist(TradeCompleteJpaEntity.builder().product(product).buyer(buyer).seller(seller)
                        .status(TradeStatus.COMPLETED).requestedBySeller(true).build());
            }
            em.flush();
            return new Fixture(product.getId(), seller.getId(), buyer.getId());
        });
    }

    private ChatMessageJpaEntity message(ChatRoomJpaEntity room, MemberJpaEntity sender) {
        return ChatMessageJpaEntity.builder().id(MESSAGE_IDS.incrementAndGet()).content("안녕하세요").messageType(MessageType.TEXT)
                .checked(false).room(room).sender(sender).createdAt(LocalDateTime.now()).build();
    }

    private MemberJpaEntity member(String name) {
        return MemberJpaEntity.builder().name(name).nickname(name).phoneNumber(name).password("test-password")
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
    }

    private record Fixture(Long productId, Long sellerId, Long buyerId) {}

    /** 운영 listener 와 같은 AFTER_COMMIT 단계에서 받은 이벤트만 기록한다. rollback 되면 받지 않는다. */
    @TestConfiguration
    static class AfterCommitRecorder {
        private final List<PushRequestedEvent> pushes = new CopyOnWriteArrayList<>();
        private final List<TradeStatusChangedEvent> statuses = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        void onPush(PushRequestedEvent event) {
            pushes.add(event);
        }

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        void onStatus(TradeStatusChangedEvent event) {
            statuses.add(event);
        }

        long pushes(Long productId) {
            return pushes.stream().filter(e -> productId.equals(e.sourceId())).count();
        }

        List<Boolean> statusChanges(Long productId) {
            return statuses.stream().filter(e -> productId.equals(e.productId())).map(TradeStatusChangedEvent::completed).toList();
        }
    }
}
