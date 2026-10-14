package team.startup.gwangsan.application.trade.service;

import jakarta.persistence.EntityManager;
import org.hibernate.resource.jdbc.spi.StatementInspector;
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
import team.startup.gwangsan.adapter.persistence.chat.ChatMessageJpaEntity;
import team.startup.gwangsan.adapter.persistence.chat.ChatRoomJpaEntity;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.notification.DeviceTokenRepository;
import team.startup.gwangsan.adapter.persistence.place.HeadJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompleteJpaEntity;
import team.startup.gwangsan.application.trade.TradeIntegrationWiring;
import team.startup.gwangsan.application.trade.data.ApproveTradeCancelCommand;
import team.startup.gwangsan.application.trade.data.RequestTradeCancelCommand;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.trade.model.TradeStatus;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 철회 승인(trade ApproveTradeCancelUseCase, admin 이 인가 콜백과 함께 호출)의 실제 SQL·락 순서를 MariaDB 에서 관찰한다.
 * 기준선 순서: TradeCancel 의 productId 스칼라 조회 → 상품 행 잠금(FOR UPDATE, 삭제 포함) → TradeCancel 최초 로드 → 거래 로드 → 인가 → 반영.
 * 상품 잠금이 TradeCancel 로드보다 먼저여야 상호 동의 철회·완료 요청과 같은 상품 잠금 순서를 공유해 교착·이중 환불을 막는다.
 */
@DataJpaTest(properties = {"spring.flyway.enabled=false",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector="
                + "team.startup.gwangsan.application.trade.service.TradeCancelApprovalLockOrderIntegrationTest$SqlRecorder"})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, TradeIntegrationWiring.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("관리자 철회 승인 락 순서 통합 테스트")
class TradeCancelApprovalLockOrderIntegrationTest {

    private static final int GWANGSAN = 700;
    private static final int BALANCE = 10_000;
    private static final AtomicLong MESSAGE_IDS = new AtomicLong(9_000);

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

    @Autowired private ApproveTradeCancelService approveTradeCancelService;
    @Autowired private RequestTradeCancelService requestTradeCancelService;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("it_productId 스칼라 조회 → 상품 FOR UPDATE → TradeCancel 로드 → 인가 순서로 실행하고 환불을 반영한다")
    void it_locks_product_before_loading_trade_cancel() {
        Fixture f = fixture();
        requestTradeCancelService.execute(new RequestTradeCancelCommand(f.sellerId, f.productId, "사유", List.of()));
        long cancelId = jdbc.queryForObject("SELECT trade_cancel_id FROM tbl_trade_cancel WHERE status = 'PENDING' AND member_id = ?",
                Long.class, f.sellerId);
        List<String> atAuthorize = new ArrayList<>();

        SqlRecorder.start();
        approveTradeCancelService.execute(new ApproveTradeCancelCommand(cancelId,
                (buyerId, sellerId) -> atAuthorize.addAll(SqlRecorder.snapshot())));
        List<String> sql = SqlRecorder.stop();

        int scalar = indexOf(sql, s -> s.startsWith("select") && s.contains("from tbl_trade_cancel")
                && s.contains("product_id") && !s.contains("for update") && !s.contains("reason"));
        int lock = indexOf(sql, s -> s.contains("from tbl_product") && s.contains("for update"));
        int loadCancel = indexOf(sql, s -> s.startsWith("select") && s.contains("from tbl_trade_cancel") && s.contains("reason"));
        assertThat(scalar).as("productId 스칼라 조회 %s", sql).isGreaterThanOrEqualTo(0);
        assertThat(lock).as("상품 잠금 %s", sql).isGreaterThan(scalar);
        assertThat(loadCancel).as("TradeCancel 최초 로드 %s", sql).isGreaterThan(lock);
        // 인가 콜백은 상품 잠금과 TradeCancel·거래 로드 이후, 반영(update) 이전에 실행된다.
        assertThat(atAuthorize).hasSizeGreaterThan(loadCancel).noneMatch(s -> s.startsWith("update"));
        assertThat(jdbc.queryForObject("SELECT status FROM tbl_trade_cancel WHERE trade_cancel_id = ?", String.class, cancelId))
                .isEqualTo("APPROVED");
        assertThat(gwangsanOf(f.buyerId)).isEqualTo(BALANCE);
        assertThat(gwangsanOf(f.sellerId)).isEqualTo(BALANCE);
    }

    private static int indexOf(List<String> sql, Predicate<String> match) {
        for (int i = 0; i < sql.size(); i++) {
            if (match.test(sql.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int gwangsanOf(long memberId) {
        return jdbc.queryForObject("SELECT gwangsan FROM tbl_member_detail WHERE member_id = ?", Integer.class, memberId);
    }

    /** 승인 실행 동안 Hibernate 가 보내는 SQL 을 순서대로 기록한다(소문자·공백 정규화). */
    public static class SqlRecorder implements StatementInspector {
        private static final List<String> RECORDED = new ArrayList<>();
        private static volatile boolean recording;

        static synchronized void start() {
            RECORDED.clear();
            recording = true;
        }

        static synchronized List<String> snapshot() {
            return List.copyOf(RECORDED);
        }

        static synchronized List<String> stop() {
            recording = false;
            return List.copyOf(RECORDED);
        }

        @Override
        public String inspect(String sql) {
            if (recording) {
                synchronized (SqlRecorder.class) {
                    RECORDED.add(sql.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim());
                }
            }
            return sql;
        }
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
            // 완료 거래가 이미 반영된 잔액에서 시작한다(철회 승인 시 구매자 환불·판매자 차감으로 BALANCE 로 돌아온다).
            em.persist(MemberDetailJpaEntity.builder().member(seller).dong(dong).gwangsan(BALANCE + GWANGSAN).place(place).light(0).description("소개").build());
            em.persist(MemberDetailJpaEntity.builder().member(buyer).dong(dong).gwangsan(BALANCE - GWANGSAN).place(place).light(0).description("소개").build());

            Product product = Product.builder().title("상품").description("설명").gwangsan(GWANGSAN).member(seller)
                    .type(Type.OBJECT).mode(Mode.GIVER).status(ProductStatus.COMPLETED).build();
            em.persist(product);

            ChatRoomJpaEntity room = ChatRoomJpaEntity.builder().product(product).buyer(buyer).seller(seller).isActive(true).build();
            em.persist(room);
            em.persist(message(room, seller));
            em.persist(message(room, buyer));
            em.persist(TradeCompleteJpaEntity.builder().product(product).buyer(buyer).seller(seller)
                    .status(TradeStatus.COMPLETED).requestedBySeller(true).build());
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
