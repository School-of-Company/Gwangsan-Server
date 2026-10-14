package team.startup.gwangsan.adapter.persistence.post;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.ReservationStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.post.model.Product;
import team.startup.gwangsan.domain.post.model.ProductReservation;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.enabled=false")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, ProductPersistenceAdapter.class, ProductImagePersistenceAdapter.class,
        ReservationPersistenceAdapter.class})
@DisplayName("게시글·예약 persistence adapter (MariaDB)")
class PostPersistenceAdapterIntegrationTest {

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

    @Autowired EntityManager em;
    @Autowired ProductPersistenceAdapter products;
    @Autowired ProductImagePersistenceAdapter images;
    @Autowired ReservationPersistenceAdapter reservations;

    private MemberJpaEntity member(String name) {
        MemberJpaEntity member = MemberJpaEntity.builder().name(name).nickname(name).phoneNumber(name).password("pw")
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
        em.persist(member);
        return member;
    }

    private Long image(String url) {
        Image image = Image.builder().imageUrl(url).build();
        em.persist(image);
        return image.getId();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("게시글 저장은")
    class Describe_product {

        @Test
        @DisplayName("새 게시글을 INSERT 하고 변경은 managed Entity 에 명시 반영한다")
        void it_inserts_and_updates() {
            MemberJpaEntity author = member("author-p");
            Product saved = products.save(Product.create(author.getId(), Type.SERVICE, Mode.GIVER, "제목", "설명", 100, List.of()));
            flushAndClear();

            Product loaded = products.lockActive(saved.getId()).orElseThrow();
            loaded.update(author.getId(), Type.OBJECT, Mode.RECEIVER, "새 제목", "새 설명", 200, List.of());
            loaded.delete(author.getId());
            products.save(loaded);
            flushAndClear();

            var entity = em.find(team.startup.gwangsan.adapter.persistence.post.Product.class, saved.getId());
            assertThat(entity.getStatus()).isEqualTo(ProductStatus.DELETED);
            assertThat(entity.getTitle()).isEqualTo("새 제목");
            assertThat(entity.getMember().getId()).isEqualTo(author.getId());
            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(products.findActive(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("이미지 교체는 빠진 연결만 지우고 새 연결을 추가하며 지운 ID 를 돌려준다")
        void it_replaces_images() {
            MemberJpaEntity author = member("author-i");
            Long keep = image("keep");
            Long drop = image("drop");
            Long add = image("add");
            Product saved = products.save(Product.create(author.getId(), Type.OBJECT, Mode.GIVER, "제목", "설명", 100, List.of(keep, drop)));
            images.addImages(saved.getId(), List.of(keep, drop));
            flushAndClear();

            Set<Long> removed = images.replaceImages(saved.getId(), List.of(keep, add));
            flushAndClear();

            assertThat(removed).containsExactly(drop);
            assertThat(images.findImages(saved.getId())).extracting(ImageView::imageId).containsExactlyInAnyOrder(keep, add);
            assertThat(images.findImages(List.of(saved.getId())).get(saved.getId())).hasSize(2);
        }
    }

    @Nested
    @DisplayName("예약 저장은")
    class Describe_reservation {

        @Test
        @DisplayName("생성·reminder 기록·취소를 저장하고 PENDING 조회/잠금 대상에서 취소 예약을 제외한다")
        void it_round_trips_reservation() {
            MemberJpaEntity author = member("author-r");
            MemberJpaEntity buyer = member("buyer-r");
            Product product = products.save(Product.create(author.getId(), Type.SERVICE, Mode.GIVER, "제목", "설명", 100, List.of()));
            LocalDateTime now = LocalDateTime.of(2026, 10, 14, 12, 0);
            ProductReservation created = reservations.save(ProductReservation.reserve(product.getId(), buyer.getId(),
                    now.plusMinutes(10), "장소", "주소", 35.1, 126.7, now));
            flushAndClear();

            assertThat(reservations.findProductIdOf(created.getId())).contains(product.getId());
            assertThat(reservations.findDueReminderIds(now.plusMinutes(10), now.plusMinutes(40))).contains(created.getId());
            ProductReservation pending = reservations.findPendingForUpdate(product.getId()).orElseThrow();
            assertThat(pending.getReminder30SentAt()).isEqualTo(now);
            pending.markReminderAtSent(now.plusMinutes(10));
            reservations.save(pending);
            flushAndClear();

            assertThat(reservations.findDueReminderIds(now.plusHours(1), now.plusHours(2))).doesNotContain(created.getId());
            ProductReservation locked = reservations.lock(created.getId()).orElseThrow();
            locked.cancel(author.getId(), author.getId());
            reservations.save(locked);
            flushAndClear();

            var entity = em.find(team.startup.gwangsan.adapter.persistence.post.ProductReservation.class, created.getId());
            assertThat(entity.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(entity.getCancelledAt()).isNotNull();
            assertThat(entity.getReminderAtSentAt()).isEqualTo(now.plusMinutes(10));
            assertThat(reservations.findPendingForUpdate(product.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("다른 업무 공개 계약용 저장은")
    class Describe_public_contract {

        @Test
        @DisplayName("삭제 포함 잠금·묶음 조회, 삭제 제외 관리자 페이지, PENDING 예약 view, 예약 완료, 이미지 연결 삭제를 처리한다")
        void it_serves_public_contract() {
            MemberJpaEntity author = member("author-c");
            MemberJpaEntity buyer = member("buyer-c");
            Long imageId = image("img-c");
            Product deleted = products.save(new Product(null, author.getId(), Type.OBJECT, Mode.GIVER, "삭제", "설명", 100, ProductStatus.DELETED));
            Product active = products.save(Product.create(author.getId(), Type.OBJECT, Mode.GIVER, "활성", "설명", 100, List.of(imageId)));
            images.addImages(active.getId(), List.of(imageId));
            LocalDateTime now = LocalDateTime.of(2026, 10, 14, 12, 0);
            ProductReservation created = reservations.save(ProductReservation.reserve(active.getId(), buyer.getId(),
                    now.plusHours(2), "장소", "주소", 35.1, 126.7, now));
            flushAndClear();

            assertThat(products.lockIncludingDeleted(deleted.getId())).get().extracting(Product::getStatus).isEqualTo(ProductStatus.DELETED);
            assertThat(products.lockActive(deleted.getId())).isEmpty();
            assertThat(products.findAll(List.of(deleted.getId(), active.getId(), -1L))).extracting(Product::getId)
                    .containsExactlyInAnyOrder(deleted.getId(), active.getId());
            assertThat(products.findActivePage(Type.OBJECT, Mode.GIVER, null, 10)).extracting(Product::getId)
                    .contains(active.getId()).doesNotContain(deleted.getId());

            var view = reservations.findPendingView(active.getId()).orElseThrow();
            assertThat(view.id()).isEqualTo(created.getId());
            assertThat(view.reserverId()).isEqualTo(buyer.getId());
            assertThat(view.createdAt()).isNotNull();
            assertThat(view.placeName()).isEqualTo("장소");

            ProductReservation pending = reservations.find(created.getId()).orElseThrow();
            pending.complete();
            reservations.save(pending);
            images.unlink(active.getId(), imageId);
            flushAndClear();

            assertThat(em.find(team.startup.gwangsan.adapter.persistence.post.ProductReservation.class, created.getId())
                    .getStatus()).isEqualTo(ReservationStatus.COMPLETED);
            assertThat(reservations.findPendingView(active.getId())).isEmpty();
            assertThat(images.findImages(active.getId())).isEmpty();
        }

        @Test
        @DisplayName("회원 탈퇴 재지정은 게시글 작성자와 예약자를 dummy 로 바꾼다")
        void it_reassigns_author_and_reserver() {
            MemberJpaEntity leaving = member("leaving");
            MemberJpaEntity other = member("other");
            MemberJpaEntity dummy = member("dummy");
            Product own = products.save(Product.create(leaving.getId(), Type.SERVICE, Mode.GIVER, "내 글", "설명", 100, List.of()));
            Product others = products.save(Product.create(other.getId(), Type.SERVICE, Mode.GIVER, "남의 글", "설명", 100, List.of()));
            LocalDateTime now = LocalDateTime.of(2026, 10, 14, 12, 0);
            ProductReservation reserved = reservations.save(ProductReservation.reserve(others.getId(), leaving.getId(),
                    now.plusHours(2), "장소", "주소", 35.1, 126.7, now));
            flushAndClear();

            products.reassignAuthor(leaving.getId(), dummy.getId());
            reservations.reassignReserver(leaving.getId(), dummy.getId());

            assertThat(products.find(own.getId()).orElseThrow().getAuthorId()).isEqualTo(dummy.getId());
            assertThat(products.find(others.getId()).orElseThrow().getAuthorId()).isEqualTo(other.getId());
            assertThat(reservations.find(reserved.getId()).orElseThrow().getReserverId()).isEqualTo(dummy.getId());
        }
    }
}
