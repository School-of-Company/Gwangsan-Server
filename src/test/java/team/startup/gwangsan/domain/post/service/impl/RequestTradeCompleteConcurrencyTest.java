package team.startup.gwangsan.domain.post.service.impl;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteException;
import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteRequestException;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 거래 완료 요청이 동시에 들어와도 유니크 제약 위반이 새어 나가지 않는지 검증한다.
 *
 * <p>결함은 REPEATABLE READ 에서 상품 잠금 전에 고정된 read view 때문에 생기므로 H2 가 아닌
 * 실제 MariaDB 컨테이너를 쓴다. 상품 행을 먼저 잠가 두고 모든 요청이 잠금 대기에 들어간 것을
 * 확인한 뒤 풀어서, 모든 요청이 앞선 커밋 이전의 스냅샷을 가진 상태를 결정적으로 만든다.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.maximum-pool-size=16"
})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, RequestTradeCompleteServiceImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("거래 완료 동시 요청 통합 테스트")
class RequestTradeCompleteConcurrencyTest {

    private static final int REQUESTS = 8;
    private static final int GWANGSAN = 100;
    private static final int BALANCE = 1_000;
    private static final AtomicLong MESSAGE_IDS = new AtomicLong();

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

    @Autowired private RequestTradeCompleteServiceImpl requestTradeCompleteService;
    @Autowired private ProductRepository productRepository;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("판매자의 동시 완료 요청은 PENDING 한 건만 만들고 나머지는 이미 요청됨으로 거절된다")
    void seller_concurrent_requests_create_single_pending() throws Exception {
        Fixture fixture = fixture(false);

        Map<String, Long> results = raceWhileProductLocked(fixture.productId(),
                () -> requestTradeCompleteService.execute(fixture.productId(), fixture.buyerId()),
                fixture.sellerPhone());

        assertThat(results).containsOnlyKeys("OK", TradeAlreadyCompleteRequestException.class.getSimpleName());
        assertThat(results.get("OK")).isEqualTo(1L);
        assertThat(countTrades(fixture.productId(), TradeStatus.PENDING)).isEqualTo(1);

        runAs(fixture.buyerPhone(),
                () -> requestTradeCompleteService.execute(fixture.productId(), fixture.sellerId()));

        assertCompletedOnce(fixture);
    }

    @Test
    @DisplayName("구매자의 동시 확정은 광산을 한 번만 이동시키고 나머지는 이미 완료됨으로 거절된다")
    void buyer_concurrent_confirms_move_gwangsan_once() throws Exception {
        Fixture fixture = fixture(true);

        Map<String, Long> results = raceWhileProductLocked(fixture.productId(),
                () -> requestTradeCompleteService.execute(fixture.productId(), fixture.sellerId()),
                fixture.buyerPhone());

        assertThat(results).containsOnlyKeys("OK", TradeAlreadyCompleteException.class.getSimpleName());
        assertThat(results.get("OK")).isEqualTo(1L);
        assertCompletedOnce(fixture);
    }

    private void assertCompletedOnce(Fixture fixture) {
        assertThat(productRepository.findById(fixture.productId()).orElseThrow().getStatus())
                .isEqualTo(ProductStatus.COMPLETED);
        assertThat(countTrades(fixture.productId(), TradeStatus.PENDING)).isZero();
        assertThat(countTrades(fixture.productId(), TradeStatus.COMPLETED)).isEqualTo(1);
        assertThat(gwangsanOf(fixture.buyerId())).isEqualTo(BALANCE - GWANGSAN);
        assertThat(gwangsanOf(fixture.sellerId())).isEqualTo(BALANCE + GWANGSAN);
    }

    /**
     * 상품 행을 잠근 채로 요청을 출발시키고, 모든 요청이 잠금 대기에 들어간 뒤 잠금을 푼다.
     * 결과는 성공 "OK" 또는 예외 클래스 이름별 건수다.
     */
    private Map<String, Long> raceWhileProductLocked(Long productId, Runnable request, String phoneNumber)
            throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(REQUESTS + 1);
        try {
            Future<?> holder = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                productRepository.findByIdWithLock(productId).orElseThrow();
                locked.countDown();
                await(release);
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                futures.add(pool.submit(() -> {
                    try {
                        runAs(phoneNumber, request);
                        return "OK";
                    } catch (RuntimeException e) {
                        return e.getClass().getSimpleName();
                    }
                }));
            }

            awaitLockWaits(REQUESTS);
            release.countDown();
            holder.get(15, TimeUnit.SECONDS);

            List<String> results = new ArrayList<>();
            for (Future<String> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void awaitLockWaits(int expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        try (var connection = DriverManager.getConnection(mariadb.getJdbcUrl(), "root", mariadb.getPassword());
             var statement = connection.createStatement()) {
            do {
                try (var rows = statement.executeQuery(
                        "SELECT COUNT(DISTINCT REQUESTING_TRX_ID) FROM information_schema.INNODB_LOCK_WAITS")) {
                    rows.next();
                    if (rows.getLong(1) >= expected) {
                        return;
                    }
                }
                // MariaDB 의 InnoDB 정보 캐시는 마지막 조회 후 100ms 넘게 쉬어야 갱신된다.
                TimeUnit.MILLISECONDS.sleep(200);
            } while (System.nanoTime() < deadline);
        }
        throw new AssertionError("모든 요청이 상품 행 잠금을 기다리지 않았습니다");
    }

    private void runAs(String phoneNumber, Runnable action) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(phoneNumber, null, List.of()));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private int countTrades(Long productId, TradeStatus status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tbl_trade_complete WHERE product_id = ? AND status = ?",
                Integer.class, productId, status.name());
    }

    private int gwangsanOf(Long memberId) {
        return jdbcTemplate.queryForObject(
                "SELECT gwangsan FROM tbl_member_detail WHERE member_id = ?", Integer.class, memberId);
    }

    private Fixture fixture(boolean withPendingBySeller) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Head head = new Head("본부" + suffix);
            em.persist(head);
            Place place = Place.builder().name("지점" + suffix).head(head).build();
            em.persist(place);
            Dong dong = Dong.builder().name("동" + suffix).build();
            em.persist(dong);

            Member seller = member("seller" + suffix);
            Member buyer = member("buyer" + suffix);
            em.persist(seller);
            em.persist(buyer);
            em.persist(memberDetail(seller, dong, place));
            em.persist(memberDetail(buyer, dong, place));

            Product product = Product.builder().title("상품").description("설명").gwangsan(GWANGSAN)
                    .member(seller).type(Type.OBJECT).mode(Mode.GIVER).status(ProductStatus.ONGOING).build();
            em.persist(product);

            ChatRoom room = ChatRoom.builder().product(product).buyer(buyer).seller(seller).isActive(true).build();
            em.persist(room);
            em.persist(message(room, seller));
            em.persist(message(room, buyer));

            if (withPendingBySeller) {
                em.persist(TradeComplete.builder().product(product).buyer(buyer).seller(seller)
                        .status(TradeStatus.PENDING).requestedBySeller(true).build());
            }
            em.flush();

            return new Fixture(product.getId(), seller.getId(), buyer.getId(),
                    seller.getPhoneNumber(), buyer.getPhoneNumber());
        });
    }

    private ChatMessage message(ChatRoom room, Member sender) {
        return ChatMessage.builder().id(MESSAGE_IDS.incrementAndGet()).content("안녕하세요").messageType(MessageType.TEXT).checked(false)
                .room(room).sender(sender).createdAt(LocalDateTime.now()).build();
    }

    private Member member(String name) {
        return Member.builder().name(name).nickname(name).phoneNumber(name).password("test-password")
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
    }

    private MemberDetail memberDetail(Member member, Dong dong, Place place) {
        return MemberDetail.builder().member(member).dong(dong).gwangsan(BALANCE)
                .place(place).light(0).description("소개").build();
    }

    private record Fixture(Long productId, Long sellerId, Long buyerId, String sellerPhone, String buyerPhone) {}
}
