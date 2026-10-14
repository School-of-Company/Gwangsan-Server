package team.startup.gwangsan.application.trade.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.adapter.event.notification.PushRequestedEventListener;
import team.startup.gwangsan.adapter.persistence.chat.ChatMessageJpaEntity;
import team.startup.gwangsan.adapter.persistence.chat.ChatRoomJpaEntity;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.notification.DeviceTokenRepository;
import team.startup.gwangsan.adapter.persistence.place.HeadJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.application.notification.port.out.LoadPushTokensPort;
import team.startup.gwangsan.application.notification.port.out.PushSenderPort;
import team.startup.gwangsan.application.notification.service.SendPushService;
import team.startup.gwangsan.application.trade.TradeIntegrationWiring;
import team.startup.gwangsan.application.trade.data.TradeCompletionCommand;
import team.startup.gwangsan.domain.alert.model.AlertType;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.trade.model.TradeStatus;
import team.startup.gwangsan.infrastructure.config.AsyncConfig;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * 커밋 이후 푸시 실패가 이미 커밋된 업무·알림을 되돌리지 않는지 실제 MariaDB 와 운영 푸시 경로
 * (PushRequestedEvent → @Async AFTER_COMMIT PushRequestedEventListener → SendPushService → PushSenderPort)로 검증한다.
 * 계획의 확정 정책: 거래 알림 2종은 업무와 같은 트랜잭션, 외부 푸시는 커밋 후 처리하며 실패해도 업무를 유지한다.
 */
@DataJpaTest(properties = "spring.flyway.enabled=false")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, TradeIntegrationWiring.class, AsyncConfig.class,
        PushRequestedEventListener.class, SendPushService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("커밋 후 푸시 실패 시 업무 보존 통합 테스트")
class TradePushFailureAfterCommitIntegrationTest {

    private static final int GWANGSAN = 700;
    private static final int BALANCE = 10_000;
    private static final AtomicLong MESSAGE_IDS = new AtomicLong(5_000);

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
    @MockitoBean private LoadPushTokensPort loadPushTokensPort;
    @MockitoBean private PushSenderPort pushSenderPort;

    @Autowired private RequestTradeCompletionService requestTradeCompletionService;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("it_요청 알림 커밋 뒤 확정 푸시 발송이 예외로 실패해도 완료 거래·잔액 이동·요청 알림은 그대로 남는다")
    void it_keeps_committed_trade_and_alert_when_push_fails_after_commit() {
        Fixture f = fixture();
        given(loadPushTokensPort.loadTokens(anyList())).willReturn(List.of("ExponentPushToken[test]"));
        given(pushSenderPort.send(any(), anyList())).willThrow(new IllegalStateException("expo down"));

        // 비작성자(구매자) 요청: PENDING + 작성자에게 완료 요청 알림(업무와 같은 트랜잭션)
        requestTradeCompletionService.execute(new TradeCompletionCommand(f.buyerId, f.productId, f.sellerId));
        // 작성자(판매자) 확정: COMPLETED + 잔액 이동, 커밋 후 TRADE_COMPLETE 푸시(비동기) — 여기서 Expo 실패
        requestTradeCompletionService.execute(new TradeCompletionCommand(f.sellerId, f.productId, f.buyerId));

        verify(pushSenderPort, timeout(5_000)).send(any(), anyList());
        assertThat(countTrades(f.productId, TradeStatus.COMPLETED)).isEqualTo(1);
        assertThat(countTrades(f.productId, TradeStatus.PENDING)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM tbl_product WHERE product_id = ?", String.class, f.productId))
                .isEqualTo(ProductStatus.COMPLETED.name());
        assertThat(gwangsanOf(f.buyerId)).isEqualTo(BALANCE - GWANGSAN);
        assertThat(gwangsanOf(f.sellerId)).isEqualTo(BALANCE + GWANGSAN);
        assertThat(alertCount(f.productId, AlertType.OTHER_MEMBER_TRADE_COMPLETE)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT r.member_id FROM tbl_alert_receipt r JOIN tbl_alert a ON a.alert_id = r.alert_id "
                + "WHERE a.source_id = ? AND a.alert_type = ?", Long.class, f.productId, AlertType.OTHER_MEMBER_TRADE_COMPLETE.name()))
                .containsExactly(f.sellerId);
    }

    private int alertCount(long sourceId, AlertType type) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_alert WHERE source_id = ? AND alert_type = ?",
                Integer.class, sourceId, type.name());
    }

    private int countTrades(long productId, TradeStatus status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_trade_complete WHERE product_id = ? AND status = ?",
                Integer.class, productId, status.name());
    }

    private int gwangsanOf(long memberId) {
        return jdbc.queryForObject("SELECT gwangsan FROM tbl_member_detail WHERE member_id = ?", Integer.class, memberId);
    }

    private Fixture fixture() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            HeadJpaEntity head = new HeadJpaEntity("본부" + suffix);
            em.persist(head);
            PlaceJpaEntity place = PlaceJpaEntity.builder().name("지점" + suffix).head(head).build();
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
                    .type(Type.OBJECT).mode(Mode.GIVER).status(ProductStatus.ONGOING).build();
            em.persist(product);

            ChatRoomJpaEntity room = ChatRoomJpaEntity.builder().product(product).buyer(buyer).seller(seller).isActive(true).build();
            em.persist(room);
            em.persist(message(room, seller));
            em.persist(message(room, buyer));
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
}
