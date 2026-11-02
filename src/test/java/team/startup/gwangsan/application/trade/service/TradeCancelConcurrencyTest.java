package team.startup.gwangsan.application.trade.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import team.startup.gwangsan.adapter.persistence.notification.DeviceTokenRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.adapter.persistence.place.HeadJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.adapter.persistence.post.ProductRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaEntity;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompleteJpaEntity;
import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.model.TradeStatus;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompleteJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.application.trade.TradeIntegrationWiring;
import team.startup.gwangsan.application.trade.data.RequestTradeCancelCommand;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 구매자와 판매자가 동시에 철회를 요청해도 광산이 정확히 한 번만 이동하는지 검증한다.
 *
 * <p>H2 로는 이 테스트를 쓸 수 없다. 이번에 막은 결함은 InnoDB 의 {@code FOR UPDATE} 동작과
 * REPEATABLE READ 스냅샷에서만 재현되는데 H2 는 둘 다 그대로 흉내 내지 않는다. 그래서
 * 실제 MariaDB 컨테이너를 띄운다.
 *
 * <p>{@code @DataJpaTest} 의 기본 트랜잭션은 꺼 둔다. 두 스레드가 각자 자기 트랜잭션을 열어야
 * 잠금 경합이 생기기 때문이다.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, TradeIntegrationWiring.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("거래 철회 동시 요청 통합 테스트")
class TradeCancelConcurrencyTest {

    private static final int GWANGSAN = 5_000;

    @Container
    static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", mariadb::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create"); // 일회용 컨테이너: 종료 시 drop 은 이미 멈춘 DB 를 기다리며 JVM 종료를 막는다
    }

    // 거래 쓰기 wiring 의 푸시 bridge 가 Redis 저장소를 요구한다. 철회 경로는 푸시를 보내지 않는다.
    @MockitoBean private DeviceTokenRepository deviceTokenRepository;

    @Autowired private RequestTradeCancelService requestTradeCancelService;
    @Autowired private TradeCompleteJpaRepository tradeCompleteRepository;
    @Autowired private TradeCancelJpaRepository tradeCancelRepository;
    @Autowired private MemberDetailJpaRepository memberDetailRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("양측이 동시에 철회를 요청해도 광산은 한 번만 이동하고 철회 요청도 한 건만 남는다")
    void it_moves_gwangsan_exactly_once() throws Exception {
        Fixture fixture = persistCompletedTrade();

        int buyerBefore = gwangsanOf(fixture.buyerId);
        int sellerBefore = gwangsanOf(fixture.sellerId);

        runConcurrently(fixture.buyer, fixture.seller, fixture.productId);

        // 광산은 정확히 1회분만 이동해야 한다. 잠금이 없으면 PENDING 이 두 건 생겨
        // 아무 이동도 일어나지 않거나(둘 다 첫 요청으로 처리), 두 번 환불된다.
        assertThat(gwangsanOf(fixture.buyerId)).isEqualTo(buyerBefore + GWANGSAN);
        assertThat(gwangsanOf(fixture.sellerId)).isEqualTo(sellerBefore - GWANGSAN);

        List<TradeCancelJpaEntity> cancels = tradeCancelRepository.findAll();
        assertThat(cancels).hasSize(1);
        assertThat(cancels.get(0).getStatus()).isEqualTo(TradeCancelStatus.APPROVED);

        TradeCompleteJpaEntity tradeComplete = tradeCompleteRepository.findById(fixture.tradeCompleteId).orElseThrow();
        assertThat(tradeComplete.getStatus()).isEqualTo(TradeStatus.ROLLED_BACK);

        Product product = productRepository.findById(fixture.productId).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ONGOING);
    }

    /** 두 요청을 같은 순간에 출발시킨다. 예외는 정상 결과다 — 한쪽이 늦게 도착하면 자동 철회가 된다. */
    private void runConcurrently(MemberJpaEntity buyer, MemberJpaEntity seller, Long productId) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicReference<Throwable> unexpected = new AtomicReference<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (MemberJpaEntity requester : List.of(buyer, seller)) {
            pool.submit(() -> {
                try {
                    start.await();
                    requestTradeCancelService.execute(
                            new RequestTradeCancelCommand(requester.getId(), productId, "동시 요청", List.of()));
                } catch (RuntimeException e) {
                    // AlreadyTradeCancelRequestException 등 도메인 예외는 허용된 결과다.
                    if (!e.getClass().getPackageName().startsWith("team.startup.gwangsan")) {
                        unexpected.set(e);
                    }
                } catch (Throwable t) {
                    unexpected.set(t);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(unexpected.get()).isNull();
    }

    private int gwangsanOf(Long memberId) {
        return memberDetailRepository.findById(memberId).orElseThrow().getGwangsan();
    }

    private record Fixture(MemberJpaEntity buyer, MemberJpaEntity seller, Long buyerId, Long sellerId,
                           Long productId, Long tradeCompleteId) {
    }

    /**
     * 픽스처는 커밋해 둬야 다른 스레드가 볼 수 있다. 테스트 자체는 트랜잭션이 꺼져 있으므로
     * TransactionTemplate 으로 별도 트랜잭션을 열어 저장한다.
     */
    private Fixture persistCompletedTrade() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            HeadJpaEntity head = new HeadJpaEntity("본부");
            entityManager.persist(head);
            PlaceJpaEntity place = PlaceJpaEntity.builder().name("지점").head(head).build();
            entityManager.persist(place);
            DongJpaEntity dong = DongJpaEntity.builder().name("공유동").build();
            entityManager.persist(dong);

            MemberJpaEntity buyer = member("구매자", "010-0000-0001");
            MemberJpaEntity seller = member("판매자", "010-0000-0002");
            entityManager.persist(buyer);
            entityManager.persist(seller);

            entityManager.persist(memberDetail(buyer, dong, place));
            entityManager.persist(memberDetail(seller, dong, place));

            Product product = Product.builder()
                    .title("상품").description("설명").gwangsan(GWANGSAN)
                    .member(seller).type(Type.SERVICE).mode(Mode.GIVER)
                    .status(ProductStatus.COMPLETED)
                    .build();
            entityManager.persist(product);

            TradeCompleteJpaEntity tradeComplete = TradeCompleteJpaEntity.builder()
                    .product(product).buyer(buyer).seller(seller)
                    .status(TradeStatus.COMPLETED).requestedBySeller(true)
                    .build();
            entityManager.persist(tradeComplete);

            entityManager.flush();

            return new Fixture(buyer, seller, buyer.getId(), seller.getId(),
                    product.getId(), tradeComplete.getId());
        });
    }

    private MemberJpaEntity member(String name, String phoneNumber) {
        return MemberJpaEntity.builder()
                .name(name).nickname(name).password("pw").phoneNumber(phoneNumber)
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE)
                .build();
    }

    private MemberDetailJpaEntity memberDetail(MemberJpaEntity member, DongJpaEntity dong, PlaceJpaEntity place) {
        return MemberDetailJpaEntity.builder()
                .member(member).dong(dong).gwangsan(GWANGSAN * 2)
                .place(place).light(0).description("소개")
                .build();
    }
}
