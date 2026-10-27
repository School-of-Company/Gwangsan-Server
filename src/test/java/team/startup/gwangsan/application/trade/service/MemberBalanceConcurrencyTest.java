package team.startup.gwangsan.application.trade.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import team.startup.gwangsan.application.trade.TradeIntegrationWiring;
import team.startup.gwangsan.application.trade.data.TradeCompletionCommand;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 회원의 서로 다른 거래가 동시에 완료될 때 회원 잔액이 유실되지 않는지 검증한다.
 *
 * <p>잠금은 상품 행에만 걸리므로 상품이 다르면 두 거래는 직렬화되지 않는다. 회원 잔액 정합성은
 * 실제 MariaDB 의 행 잠금·스냅샷 동작에 달려 있어 Testcontainers 를 쓴다.
 * 벤치 테스트는 결과를 build/bench/*.csv 에 남기고, 반복 횟수는 -Dbench.iterations 로 받는다.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.maximum-pool-size=16"
})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, TradeIntegrationWiring.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("회원 잔액 동시성 통합 테스트")
class MemberBalanceConcurrencyTest {

    private static final int SELLERS = 5;
    private static final int GWANGSAN = 100;
    private static final int BALANCE = 10_000;
    private static final int CROSSING_ROUNDS = 20;
    private static final AtomicLong MESSAGE_IDS = new AtomicLong();
    private static final Path BENCH_DIR = Path.of("build", "bench");

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

    @Autowired private RequestTradeCompletionService requestTradeCompletionService;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    @Nested
    @DisplayName("같은 구매자의 서로 다른 상품을 동시에 완료하면")
    class Describe_concurrent_completions {

        @Test
        @DisplayName("T1 구매자 행이 잠긴 동안 쌓인 5건이 풀려도 잔액 합이 보존된다")
        void it_preserves_total_on_concurrent_completions() throws Exception {
            Fixture f = fixture(SELLERS);
            long totalBefore = totalOf(f);

            List<Outcome> outcomes = race(f, () -> holdDetailLockUntilWaiting(f.buyerId(), SELLERS));

            assertThat(outcomes).extracting(Outcome::error).containsOnlyNulls();
            assertThat(balanceOf(f.buyerId())).isEqualTo(BALANCE - SELLERS * GWANGSAN);
            assertThat(totalOf(f)).isEqualTo(totalBefore);
        }

        @Test
        @Tag("bench")
        @DisplayName("T1' 잠금 유도 없이 자연 경합을 N회 반복해도 광산이 생겨나지 않는다")
        void it_preserves_total_under_natural_race() throws Exception {
            int iterations = Integer.getInteger("bench.iterations", 1);
            Path csv = benchFile("lost-update.csv", "iteration,created,cumulative");
            Path latency = benchFile("lost-update-latency.csv", "iteration,request,millis,error");
            long cumulativeCreated = 0;
            int lostIterations = 0;
            List<Long> millis = new ArrayList<>();
            int deadlocks = 0;

            for (int i = 1; i <= iterations; i++) {
                Fixture f = fixture(SELLERS);
                long totalBefore = totalOf(f);
                List<Outcome> outcomes = race(f, null);
                long created = totalOf(f) - totalBefore;
                cumulativeCreated += created;
                if (created != 0) {
                    lostIterations++;
                }
                append(csv, i + "," + created + "," + cumulativeCreated);
                for (int r = 0; r < outcomes.size(); r++) {
                    Outcome o = outcomes.get(r);
                    millis.add(o.millis());
                    if (o.isDeadlock()) {
                        deadlocks++;
                    }
                    append(latency, i + "," + r + "," + o.millis() + "," + (o.error() == null ? "" : o.error()));
                }
            }
            System.out.printf("[BENCH lost-update] iterations=%d lostIterations=%d cumulativeCreated=%d p50=%dms p99=%dms deadlocks=%d%n",
                    iterations, lostIterations, cumulativeCreated, percentile(millis, 50), percentile(millis, 99), deadlocks);

            assertThat(cumulativeCreated).isZero();
        }
    }

    @Nested
    @DisplayName("A→B, B→A 맞거래를 동시에 완료하면")
    class Describe_crossing_trades {

        @Test
        @DisplayName("T3 데드락 없이 둘 다 성공하고 두 회원 잔액 합이 보존된다")
        void it_completes_crossing_trades_without_deadlock() throws Exception {
            List<String> failures = new ArrayList<>();
            List<Long> millis = new ArrayList<>();
            int deadlocks = 0;
            for (int round = 0; round < CROSSING_ROUNDS; round++) {
                Crossing c = crossing();
                long totalBefore = (long) balanceOf(c.a()) + balanceOf(c.b());

                List<Outcome> outcomes = runConcurrently(List.of(
                        () -> complete(c.a(), c.productOfA(), c.b()),
                        () -> complete(c.b(), c.productOfB(), c.a())), null);

                for (Outcome o : outcomes) {
                    millis.add(o.millis());
                    if (o.error() != null) {
                        failures.add("round " + round + ": " + o.error());
                    }
                    if (o.isDeadlock()) {
                        deadlocks++;
                    }
                }
                if (balanceOf(c.a()) + balanceOf(c.b()) != totalBefore) {
                    failures.add("round " + round + ": 합계 불일치");
                }
            }
            System.out.printf("[BENCH crossing] rounds=%d failures=%d deadlocks=%d p50=%dms p99=%dms%n",
                    CROSSING_ROUNDS, failures.size(), deadlocks, percentile(millis, 50), percentile(millis, 99));

            assertThat(failures).isEmpty();
        }
    }

    private void complete(long authorId, long productId, long buyerId) {
        requestTradeCompletionService.execute(new TradeCompletionCommand(authorId, productId, buyerId));
    }

    /** 판매자(작성자)마다 자기 상품을 같은 구매자와 동시에 완료한다. blocker 가 있으면 함께 띄운다. */
    private List<Outcome> race(Fixture f, Runnable blocker) throws Exception {
        List<Runnable> requests = new ArrayList<>();
        for (int i = 0; i < f.sellerIds().size(); i++) {
            long sellerId = f.sellerIds().get(i);
            long productId = f.productIds().get(i);
            requests.add(() -> complete(sellerId, productId, f.buyerId()));
        }
        return runConcurrently(requests, blocker);
    }

    /**
     * 시작 래치로 요청을 한꺼번에 출발시킨다. blocker 는 요청보다 먼저 시작해 잠금을 잡고,
     * 요청이 모두 대기에 들어간 뒤 풀어서 모든 요청이 같은 옛 잔액을 읽은 상태를 결정적으로 만든다.
     */
    private List<Outcome> runConcurrently(List<Runnable> requests, Runnable blocker) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests.size() + 1);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> holder = null;
            if (blocker != null) {
                CountDownLatch locked = new CountDownLatch(1);
                blockerLocked = locked;
                holder = pool.submit(blocker);
                assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            }
            List<Future<Outcome>> futures = new ArrayList<>();
            for (Runnable request : requests) {
                futures.add(pool.submit(() -> {
                    start.await();
                    long begin = System.nanoTime();
                    try {
                        request.run();
                        return new Outcome(elapsedMillis(begin), null);
                    } catch (RuntimeException e) {
                        return new Outcome(elapsedMillis(begin), describe(e));
                    }
                }));
            }
            start.countDown();
            if (holder != null) {
                holder.get(30, TimeUnit.SECONDS);
            }
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(60, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }
    }

    private volatile CountDownLatch blockerLocked;

    /** 회원 상세 행을 FOR UPDATE 로 잡고, 요청 expected 건이 잠금 대기에 들어갈 때까지 기다린 뒤 푼다. */
    private void holdDetailLockUntilWaiting(long memberId, int expected) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbcTemplate.queryForObject("SELECT gwangsan FROM tbl_member_detail WHERE member_id = ? FOR UPDATE",
                    Integer.class, memberId);
            blockerLocked.countDown();
            try {
                awaitLockWaits(expected);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
    }

    private void awaitLockWaits(int expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
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
        throw new AssertionError("모든 요청이 회원 상세 행 잠금을 기다리지 않았습니다");
    }

    long totalOf(Fixture f) {
        long total = balanceOf(f.buyerId());
        for (long sellerId : f.sellerIds()) {
            total += balanceOf(sellerId);
        }
        return total;
    }

    private int balanceOf(long memberId) {
        return jdbcTemplate.queryForObject(
                "SELECT gwangsan FROM tbl_member_detail WHERE member_id = ?", Integer.class, memberId);
    }

    /** 구매자 1명과 판매자 sellers 명. 판매자마다 자기 GIVER 상품·채팅방·판매자 메시지를 갖는다. */
    Fixture fixture(int sellers) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            Place place = place();
            MemberJpaEntity buyer = persistMember("buyer", place);
            List<Long> sellerIds = new ArrayList<>();
            List<Long> productIds = new ArrayList<>();
            for (int i = 0; i < sellers; i++) {
                MemberJpaEntity seller = persistMember("seller", place);
                productIds.add(persistTradeable(seller, buyer));
                sellerIds.add(seller.getId());
            }
            em.flush();
            return new Fixture(buyer.getId(), sellerIds, productIds);
        });
    }

    /** A 의 상품을 B 가, B 의 상품을 A 가 산다. 완료는 각 작성자가 한다. */
    private Crossing crossing() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            Place place = place();
            MemberJpaEntity a = persistMember("a", place);
            MemberJpaEntity b = persistMember("b", place);
            long productOfA = persistTradeable(a, b);
            long productOfB = persistTradeable(b, a);
            em.flush();
            return new Crossing(a.getId(), b.getId(), productOfA, productOfB);
        });
    }

    private Place place() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        HeadJpaEntity head = new HeadJpaEntity("본부" + suffix);
        em.persist(head);
        PlaceJpaEntity place = PlaceJpaEntity.builder().name("지점" + suffix).head(head).build();
        em.persist(place);
        DongJpaEntity dong = DongJpaEntity.builder().name("동" + suffix).build();
        em.persist(dong);
        return new Place(place, dong);
    }

    private MemberJpaEntity persistMember(String prefix, Place place) {
        String name = prefix + UUID.randomUUID().toString().substring(0, 8);
        MemberJpaEntity member = MemberJpaEntity.builder().name(name).nickname(name).phoneNumber(name)
                .password("test-password").role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
        em.persist(member);
        em.persist(MemberDetailJpaEntity.builder().member(member).dong(place.dong()).gwangsan(BALANCE)
                .place(place.place()).light(0).description("소개").build());
        return member;
    }

    /** seller 가 작성한 GIVER 상품과 buyer 와의 채팅방, seller 의 메시지를 만든다. */
    private long persistTradeable(MemberJpaEntity seller, MemberJpaEntity buyer) {
        Product product = Product.builder().title("상품").description("설명").gwangsan(GWANGSAN)
                .member(seller).type(Type.OBJECT).mode(Mode.GIVER).status(ProductStatus.ONGOING).build();
        em.persist(product);
        ChatRoomJpaEntity room = ChatRoomJpaEntity.builder().product(product).buyer(buyer).seller(seller).isActive(true).build();
        em.persist(room);
        em.persist(ChatMessageJpaEntity.builder().id(MESSAGE_IDS.incrementAndGet()).content("안녕하세요")
                .messageType(MessageType.TEXT).checked(false).room(room).sender(seller).createdAt(LocalDateTime.now()).build());
        return product.getId();
    }

    private static String describe(Throwable e) {
        StringBuilder sb = new StringBuilder(e.getClass().getSimpleName());
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains("Deadlock")) {
                sb.append("[Deadlock]");
                break;
            }
        }
        return sb.toString();
    }

    private static long elapsedMillis(long begin) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
    }

    private static long percentile(List<Long> values, int p) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    private static Path benchFile(String name, String header) throws IOException {
        Files.createDirectories(BENCH_DIR);
        Path file = BENCH_DIR.resolve(name);
        Files.writeString(file, header + System.lineSeparator());
        return file;
    }

    private static void append(Path file, String line) {
        try {
            Files.writeString(file, line + System.lineSeparator(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record Fixture(long buyerId, List<Long> sellerIds, List<Long> productIds) {}

    private record Crossing(long a, long b, long productOfA, long productOfB) {}

    private record Place(PlaceJpaEntity place, DongJpaEntity dong) {}

    private record Outcome(long millis, String error) {
        boolean isDeadlock() {
            return error != null && error.contains("[Deadlock]");
        }
    }
}
