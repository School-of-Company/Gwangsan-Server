package team.startup.gwangsan.domain.post.service.impl;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.ReservedProductDeletionException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.service.TradeStateReader;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;
import team.startup.gwangsan.global.util.MemberUtil;

import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = "spring.flyway.enabled=false")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, DeleteProductByIdServiceImpl.class,
        ReservationProductServiceImpl.class, TradeStateReader.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("MariaDB 예약과 작성자 삭제 직렬화 통합 테스트")
class ProductReservationDeletionIntegrationTest {

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

    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ProductRepository productRepository;
    @Autowired private DeleteProductByIdServiceImpl deleteService;
    @Autowired private ReservationProductServiceImpl reservationService;
    @MockitoBean private MemberUtil memberUtil;

    @Nested
    @DisplayName("예약과 삭제가 경합하면")
    class Describe_concurrent_requests {

        @Test
        @DisplayName("예약이 잠금을 먼저 잡으면 삭제는 대기 후 예약 삭제 에러로 거절된다")
        void it_rejects_deletion_when_reservation_wins() throws Exception {
            Fixture fixture = fixture(ProductStatus.ONGOING);
            when(memberUtil.getCurrentMember()).thenReturn(fixture.author());

            race(fixture, () -> reserve(fixture),
                    () -> deleteService.execute(fixture.productId()), ReservedProductDeletionException.class);

            assertState(fixture, ProductStatus.RESERVATION, 1L);
        }

        @Test
        @DisplayName("삭제가 잠금을 먼저 잡으면 예약은 대기 후 없는 상품으로 거절된다")
        void it_rejects_reservation_when_deletion_wins() throws Exception {
            Fixture fixture = fixture(ProductStatus.ONGOING);
            when(memberUtil.getCurrentMember()).thenReturn(fixture.author());

            race(fixture, () -> deleteService.execute(fixture.productId()),
                    () -> reserve(fixture), NotFoundProductException.class);

            assertState(fixture, ProductStatus.DELETED, 0L);
        }

    }

    @Nested
    @DisplayName("작성자가 일반 삭제를 요청하면")
    class Describe_author_deletion {

        @ParameterizedTest
        @EnumSource(value = ProductStatus.class, names = {"ONGOING", "COMPLETED"})
        @DisplayName("미예약 상품은 논리 삭제 상태로 커밋한다")
        void it_commits_soft_deletion(ProductStatus status) {
            Fixture fixture = fixture(status);
            when(memberUtil.getCurrentMember()).thenReturn(fixture.author());

            deleteService.execute(fixture.productId());

            assertState(fixture, ProductStatus.DELETED, 0L);
            assertThat(productRepository.findActiveById(fixture.productId())).isEmpty();
        }

        @Test
        @DisplayName("예약 상품도 비작성자에게는 권한 에러가 우선하며 상태를 보존한다")
        void it_rejects_non_author_before_reservation_validation() {
            Fixture fixture = fixture(ProductStatus.RESERVATION);
            Member buyer = new TransactionTemplate(transactionManager).execute(status ->
                    em.find(ChatRoom.class, fixture.roomId()).getBuyer());
            when(memberUtil.getCurrentMember()).thenReturn(buyer);
            var before = snapshot(fixture);

            assertThatThrownBy(() -> deleteService.execute(fixture.productId()))
                    .isInstanceOf(ForbiddenProductException.class);

            assertThat(snapshot(fixture)).isEqualTo(before);
        }

        @Test
        @DisplayName("없는 상품 삭제는 없는 상품 에러로 거절한다")
        void it_rejects_missing_product() {
            Fixture fixture = fixture(ProductStatus.ONGOING);
            when(memberUtil.getCurrentMember()).thenReturn(fixture.author());

            assertThatThrownBy(() -> deleteService.execute(Long.MAX_VALUE))
                    .isInstanceOf(NotFoundProductException.class);

            assertState(fixture, ProductStatus.ONGOING, 0L);
        }

        @Test
        @DisplayName("이미 삭제된 상품은 없는 상품 에러로 거절하고 행을 보존한다")
        void it_rejects_already_deleted_product() {
            Fixture fixture = fixture(ProductStatus.DELETED);
            when(memberUtil.getCurrentMember()).thenReturn(fixture.author());
            var before = snapshot(fixture);

            assertThatThrownBy(() -> deleteService.execute(fixture.productId()))
                    .isInstanceOf(NotFoundProductException.class);

            assertThat(snapshot(fixture)).isEqualTo(before);
        }

        @Test
        @DisplayName("예약 삭제 거절은 상품 및 이미지·채팅·예약·거래·리뷰를 보존한다")
        void it_preserves_all_columns_when_reserved_deletion_is_rejected() {
            Fixture fixture = fixture(ProductStatus.RESERVATION);
            when(memberUtil.getCurrentMember()).thenReturn(fixture.author());
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                Product product = em.find(Product.class, fixture.productId());
                Member buyer = em.find(ChatRoom.class, fixture.roomId()).getBuyer();
                Image image = Image.builder().imageUrl("https://example.com/image.jpg").build();
                em.persist(image);
                ProductImage link = ProductImage.builder().product(product).image(image).build();
                em.persist(link);
                ProductReservation reservation = ProductReservation.builder().product(product).reserver(buyer)
                        .status(ReservationStatus.PENDING).scheduledAt(LocalDateTime.now().plusDays(1))
                        .placeName("약속 장소").address("보존할 주소").latitude(35.0).longitude(126.0).build();
                em.persist(reservation);
                ProductReservation cancelled = ProductReservation.builder().product(product).reserver(buyer)
                        .scheduledAt(LocalDateTime.now().minusDays(1)).placeName("취소한 장소").build();
                cancelled.cancel();
                em.persist(cancelled);
                TradeComplete trade = TradeComplete.builder().product(product).buyer(buyer)
                        .seller(product.getMember()).status(TradeStatus.COMPLETED).build();
                em.persist(trade);
                Review review = Review.builder().product(product).reviewer(buyer).reviewed(product.getMember())
                        .content("보존할 후기").light(5).build();
                em.persist(review);
                em.flush();
            });
            var before = snapshot(fixture);

            assertThatThrownBy(() -> deleteService.execute(fixture.productId()))
                    .isInstanceOf(ReservedProductDeletionException.class);

            assertState(fixture, ProductStatus.RESERVATION, 2L);
            assertThat(snapshot(fixture)).isEqualTo(before);
        }

    }

    private List<List<Map<String, Object>>> snapshot(Fixture fixture) {
        return List.of(
                jdbcTemplate.queryForList("SELECT * FROM tbl_product WHERE product_id = ?", fixture.productId()),
                jdbcTemplate.queryForList("SELECT * FROM tbl_product_reservation WHERE product_id = ? ORDER BY product_reservation_id", fixture.productId()),
                jdbcTemplate.queryForList("SELECT * FROM tbl_product_image WHERE product_id = ? ORDER BY product_image_id", fixture.productId()),
                jdbcTemplate.queryForList("SELECT i.* FROM tbl_image i JOIN tbl_product_image p ON p.image_id = i.image_id WHERE p.product_id = ? ORDER BY i.image_id", fixture.productId()),
                jdbcTemplate.queryForList("SELECT * FROM tbl_chat_room WHERE product_id = ? ORDER BY room_id", fixture.productId()),
                jdbcTemplate.queryForList("SELECT * FROM tbl_trade_complete WHERE product_id = ? ORDER BY trade_id", fixture.productId()),
                jdbcTemplate.queryForList("SELECT * FROM tbl_review WHERE product_id = ? ORDER BY review_id", fixture.productId())
        );
    }

    private void race(Fixture fixture, Runnable winner, Runnable loser,
                      Class<? extends Throwable> expected) throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                productRepository.findByIdWithLock(fixture.productId()).orElseThrow();
                locked.countDown();
                await(release);
                winner.run();
            }));
            if (!locked.await(10, TimeUnit.SECONDS)) {
                if (first.isDone()) {
                    first.get();
                }
                throw new AssertionError("첫 번째 작업이 상품 행 잠금을 획득하지 못했습니다");
            }
            // 이전 조회 후 100ms 넘게 쉬고 빈 캐시를 먼저 읽어, 대기 감지 회귀를 검증한다.
            TimeUnit.MILLISECONDS.sleep(200);
            try (var connection = DriverManager.getConnection(mariadb.getJdbcUrl(), "root", mariadb.getPassword());
                 var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT COUNT(*) FROM information_schema.INNODB_LOCK_WAITS")) {
                rows.next();
                assertThat(rows.getLong(1)).as("두 번째 작업 시작 전 빈 잠금 대기 캐시").isZero();
            }
            var second = pool.submit(() -> assertThatThrownBy(loser::run).isInstanceOf(expected));
            awaitDatabaseLockWait(first, second);
            assertThat(second.isDone()).isFalse();
            release.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void awaitDatabaseLockWait(Future<?> first, Future<?> second) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        try (var connection = DriverManager.getConnection(mariadb.getJdbcUrl(), "root", mariadb.getPassword());
             var statement = connection.createStatement()) {
            do {
                if (first.isDone()) {
                    first.get();
                    throw new AssertionError("잠금 해제 전에 첫 번째 작업이 종료되었습니다");
                }
                if (second.isDone()) {
                    second.get();
                    throw new AssertionError("DB 잠금 대기 확인 전에 두 번째 작업이 종료되었습니다");
                }
                try (var rows = statement.executeQuery("SELECT COUNT(*) FROM information_schema.INNODB_LOCK_WAITS")) {
                    rows.next();
                    if (rows.getLong(1) > 0) {
                        return;
                    }
                }
                // MariaDB의 InnoDB 정보 캐시는 마지막 조회 후 100ms 넘게 쉬어야 갱신된다.
                // 25ms 폴링은 빈 스냅샷을 계속 유지하므로 실제 행 잠금 대기를 놓친다.
                TimeUnit.MILLISECONDS.sleep(200);
            } while (System.nanoTime() < deadline);
        }
        throw new AssertionError("두 번째 서비스가 MariaDB 행 잠금을 기다리지 않았습니다");
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private void reserve(Fixture fixture) {
        reservationService.execute(fixture.productId(), fixture.roomId(), LocalDateTime.now().plusDays(1),
                "약속 장소", "주소", 35.0, 126.0);
    }

    private void assertState(Fixture fixture, ProductStatus expected, long reservations) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(em.find(Product.class, fixture.productId()).getStatus()).isEqualTo(expected);
            assertThat(em.find(ChatRoom.class, fixture.roomId()).getIsActive()).isTrue();
            assertThat(em.createQuery("select count(r) from ProductReservation r where r.product.id = :id", Long.class)
                    .setParameter("id", fixture.productId()).getSingleResult()).isEqualTo(reservations);
        });
    }

    private Fixture fixture(ProductStatus productStatus) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
            Member author = member("author" + suffix);
            Member buyer = member("buyer" + suffix);
            em.persist(author);
            em.persist(buyer);
            Product product = Product.builder().title("상품").description("설명").gwangsan(100)
                    .member(author).status(productStatus).type(Type.OBJECT).mode(Mode.GIVER).build();
            em.persist(product);
            ChatRoom room = ChatRoom.builder().product(product).buyer(buyer).seller(author).isActive(true).build();
            em.persist(room);
            em.flush();
            return new Fixture(author, product.getId(), room.getId());
        });
    }

    private Member member(String name) {
        return Member.builder().name(name).nickname(name).phoneNumber(name).password("test-password")
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
    }

    private record Fixture(Member author, Long productId, Long roomId) {}
}
