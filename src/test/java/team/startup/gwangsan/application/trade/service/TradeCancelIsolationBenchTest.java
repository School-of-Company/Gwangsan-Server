package team.startup.gwangsan.application.trade.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.notification.DeviceTokenRepository;
import team.startup.gwangsan.adapter.persistence.place.HeadJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompleteJpaEntity;
import team.startup.gwangsan.application.trade.TradeIntegrationWiring;
import team.startup.gwangsan.application.trade.data.RequestTradeCancelCommand;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.trade.model.TradeStatus;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #387 반복 측정 하네스. 양측이 동시에 철회를 요청하는 상황을 N회 반복해 결과를 집계한다.
 *
 * <p>REPEATABLE READ 결함은 상품 잠금 전에 실제 SELECT 가 있어야 드러난다(그 시점에 read view 가 고정된다).
 * 헥사고날 전환 후 서비스는 잠금이 첫 문장이라, 전환 전 MemberUtil 의 회원 조회를 흉내 내려고
 * 서비스와 같은 격리 수준의 바깥 트랜잭션에서 회원을 먼저 SELECT 한 뒤 서비스를 호출한다(서비스는 참여).
 * 격리 수준은 {@link RequestTradeCancelService#execute} 의 선언값을 그대로 읽는다.
 * -Dbench.preSelect=false 면 바깥 트랜잭션 없이 서비스를 바로 호출한다(현재 운영 경로).
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, TradeIntegrationWiring.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("거래 철회 격리 수준 반복 측정")
class TradeCancelIsolationBenchTest {

    private static final int GWANGSAN = 5_000;
    private static final int BALANCE = GWANGSAN * 2;

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

    @MockitoBean private DeviceTokenRepository deviceTokenRepository;

    @Autowired private RequestTradeCancelService requestTradeCancelService;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    @Tag("bench")
    @DisplayName("양측 동시 철회를 N회 반복해 correct / doubleRefund / noMove / other 로 집계한다")
    void it_counts_outcomes_of_concurrent_cancel_requests() throws Exception {
        int iterations = Integer.getInteger("bench.iterations", 1);
        boolean preSelect = Boolean.parseBoolean(System.getProperty("bench.preSelect", "true"));
        Isolation isolation = RequestTradeCancelService.class.getMethod("execute", RequestTradeCancelCommand.class)
                .getAnnotation(Transactional.class).isolation();

        Map<String, Integer> counts = new TreeMap<>(Map.of("correct", 0, "doubleRefund", 0, "noMove", 0, "other", 0));
        for (int i = 0; i < iterations; i++) {
            Fixture f = persistCompletedTrade();
            race(f, isolation, preSelect);
            counts.merge(classify(f), 1, Integer::sum);
        }

        Path csv = Path.of("build", "bench", "387-isolation.csv");
        Files.createDirectories(csv.getParent());
        if (Files.notExists(csv)) {
            Files.writeString(csv, "isolation,preSelect,iterations,correct,doubleRefund,noMove,other\n");
        }
        Files.writeString(csv, "%s,%s,%d,%d,%d,%d,%d%n".formatted(isolation, preSelect, iterations,
                counts.get("correct"), counts.get("doubleRefund"), counts.get("noMove"), counts.get("other")),
                java.nio.file.StandardOpenOption.APPEND);
        System.out.printf("[BENCH 387] isolation=%s preSelect=%s iterations=%d %s%n", isolation, preSelect, iterations, counts);

        assertThat(counts.get("correct")).isEqualTo(iterations);
    }

    private String classify(Fixture f) {
        int moved = gwangsanOf(f.buyerId()) - BALANCE;
        int cancels = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tbl_trade_cancel WHERE trade_id = ?", Integer.class, f.tradeCompleteId());
        if (moved == GWANGSAN && cancels == 1) {
            return "correct";
        }
        if (moved == 2 * GWANGSAN) {
            return "doubleRefund";
        }
        if (moved == 0) {
            return "noMove";
        }
        return "other";
    }

    private void race(Fixture f, Isolation isolation, boolean preSelect) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        outer.setIsolationLevel(isolation.value());
        for (long requester : List.of(f.buyerId(), f.sellerId())) {
            pool.submit(() -> {
                try {
                    start.await();
                    Runnable call = () -> requestTradeCancelService.execute(
                            new RequestTradeCancelCommand(requester, f.productId(), "동시 요청", List.of()));
                    if (preSelect) {
                        outer.executeWithoutResult(status -> {
                            jdbcTemplate.queryForObject("SELECT nickname FROM tbl_member WHERE member_id = ?",
                                    String.class, requester);
                            call.run();
                        });
                    } else {
                        call.run();
                    }
                } catch (Exception e) {
                    // 도메인 예외(이미 요청됨 등)는 허용된 결과다. 집계는 잔액으로 한다.
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
    }

    private int gwangsanOf(long memberId) {
        return jdbcTemplate.queryForObject(
                "SELECT gwangsan FROM tbl_member_detail WHERE member_id = ?", Integer.class, memberId);
    }

    private record Fixture(long buyerId, long sellerId, long productId, long tradeCompleteId) {}

    private Fixture persistCompletedTrade() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            HeadJpaEntity head = new HeadJpaEntity("본부" + suffix);
            entityManager.persist(head);
            PlaceJpaEntity place = PlaceJpaEntity.builder().name("지점" + suffix).head(head).build();
            entityManager.persist(place);
            DongJpaEntity dong = DongJpaEntity.builder().name("동" + suffix).build();
            entityManager.persist(dong);

            MemberJpaEntity buyer = member("buyer" + suffix);
            MemberJpaEntity seller = member("seller" + suffix);
            entityManager.persist(buyer);
            entityManager.persist(seller);
            entityManager.persist(memberDetail(buyer, dong, place));
            entityManager.persist(memberDetail(seller, dong, place));

            Product product = Product.builder().title("상품").description("설명").gwangsan(GWANGSAN)
                    .member(seller).type(Type.SERVICE).mode(Mode.GIVER).status(ProductStatus.COMPLETED).build();
            entityManager.persist(product);

            TradeCompleteJpaEntity tradeComplete = TradeCompleteJpaEntity.builder()
                    .product(product).buyer(buyer).seller(seller)
                    .status(TradeStatus.COMPLETED).requestedBySeller(true).build();
            entityManager.persist(tradeComplete);
            entityManager.flush();

            return new Fixture(buyer.getId(), seller.getId(), product.getId(), tradeComplete.getId());
        });
    }

    private MemberJpaEntity member(String name) {
        return MemberJpaEntity.builder().name(name).nickname(name).password("pw").phoneNumber(name)
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
    }

    private MemberDetailJpaEntity memberDetail(MemberJpaEntity member, DongJpaEntity dong, PlaceJpaEntity place) {
        return MemberDetailJpaEntity.builder().member(member).dong(dong).gwangsan(BALANCE)
                .place(place).light(0).description("소개").build();
    }
}
