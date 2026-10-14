package team.startup.gwangsan.adapter.integration.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.adapter.persistence.chat.ChatRoomJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.ProductImagePersistenceAdapter;
import team.startup.gwangsan.application.admin.service.DeleteAdminProductService;
import team.startup.gwangsan.application.post.service.ProductPublicQueryService;
import team.startup.gwangsan.application.post.service.ProductStatusService;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.adapter.persistence.post.ProductImage;
import team.startup.gwangsan.adapter.persistence.post.ProductReservation;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.ReservationStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.adapter.persistence.post.ProductRepository;
import team.startup.gwangsan.adapter.persistence.post.ProductPersistenceAdapter;
import team.startup.gwangsan.application.post.service.DeleteProductService;
import team.startup.gwangsan.adapter.persistence.review.ReviewJpaEntity;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompleteJpaEntity;
import team.startup.gwangsan.domain.trade.model.TradeStatus;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.enabled=false")
@Import({QueryDslConfig.class, DeleteAdminProductService.class, AdminProductBridge.class, ProductStatusService.class,
        ProductPublicQueryService.class, ProductImagePersistenceAdapter.class, DeleteProductService.class,
        team.startup.gwangsan.application.member.service.MemberSummaryService.class,
        team.startup.gwangsan.adapter.persistence.member.MemberPersistenceAdapter.class,
        ProductPersistenceAdapter.class})
@DisplayName("관리자 상품 삭제 통합 테스트")
class AdminProductDeleteIntegrationTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private DeleteAdminProductService adminService;
    @Autowired
    private DeleteProductService userService;
    @Autowired
    private ProductRepository productRepository;

    private MemberJpaEntity author;

    @BeforeEach
    void setUp() {
        author = persistMember("author", MemberRole.ROLE_USER);
    }

    @Nested
    @DisplayName("관리자 삭제 경로는")
    class Describe_admin_delete {

        @ParameterizedTest
        @EnumSource(value = ProductStatus.class, names = {"ONGOING", "RESERVATION", "COMPLETED"})
        @DisplayName("상태만 삭제로 변경하고 이미지·채팅·거래·리뷰·예약을 보존한다")
        void it_soft_deletes_and_preserves_all_related_data(ProductStatus status) {
            Product product = persistProduct(status);
            MemberJpaEntity buyer = persistMember("buyer", MemberRole.ROLE_USER);
            ImageJpaEntity image = em.persist(ImageJpaEntity.builder().imageUrl("https://example.com/product.jpg").build());
            ProductImage link = em.persist(ProductImage.builder().product(product).image(image).build());
            ChatRoomJpaEntity room = em.persist(ChatRoomJpaEntity.builder().product(product).buyer(buyer).seller(author)
                    .isActive(true).build());
            TradeCompleteJpaEntity trade = em.persist(TradeCompleteJpaEntity.builder().product(product).buyer(buyer)
                    .seller(author).status(TradeStatus.COMPLETED).build());
            ReviewJpaEntity review = em.persist(ReviewJpaEntity.builder().product(product).reviewer(buyer).reviewed(author)
                    .content("거래 후기").light(5).build());
            ProductReservation reservation = em.persist(ProductReservation.builder().product(product)
                    .reserver(buyer).status(ReservationStatus.PENDING).build());
            em.flush();
            em.clear();

            adminService.execute(product.getId());
            em.flush();
            em.clear();

            assertThat(em.find(Product.class, product.getId()).getStatus()).isEqualTo(ProductStatus.DELETED);
            assertThat(productRepository.findActiveById(product.getId())).isEmpty();
            assertThat(em.find(ImageJpaEntity.class, image.getId()).getImageUrl()).isEqualTo(image.getImageUrl());
            ProductImage storedLink = em.find(ProductImage.class, link.getId());
            assertThat(storedLink.getProduct().getId()).isEqualTo(product.getId());
            assertThat(storedLink.getImage().getId()).isEqualTo(image.getId());
            ChatRoomJpaEntity storedRoom = em.find(ChatRoomJpaEntity.class, room.getId());
            assertThat(storedRoom.getProduct().getId()).isEqualTo(product.getId());
            assertThat(storedRoom.getIsActive()).isTrue();
            TradeCompleteJpaEntity storedTrade = em.find(TradeCompleteJpaEntity.class, trade.getId());
            assertThat(storedTrade.getProduct().getId()).isEqualTo(product.getId());
            assertThat(storedTrade.getStatus()).isEqualTo(TradeStatus.COMPLETED);
            ReviewJpaEntity storedReview = em.find(ReviewJpaEntity.class, review.getId());
            assertThat(storedReview.getProduct().getId()).isEqualTo(product.getId());
            assertThat(storedReview.getContent()).isEqualTo("거래 후기");
            ProductReservation storedReservation = em.find(ProductReservation.class, reservation.getId());
            assertThat(storedReservation.getProduct().getId()).isEqualTo(product.getId());
            assertThat(storedReservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        @DisplayName("없는 상품 삭제를 거부한다")
        void it_rejects_missing_product() {
            assertThatThrownBy(() -> adminService.execute(Long.MAX_VALUE))
                    .isInstanceOf(NotFoundProductException.class);
        }

        @Test
        @DisplayName("이미 삭제된 상품 삭제를 거부한다")
        void it_rejects_already_deleted_product() {
            Product product = persistProduct(ProductStatus.DELETED);
            assertThatThrownBy(() -> adminService.execute(product.getId()))
                    .isInstanceOf(NotFoundProductException.class);
            assertThat(em.find(Product.class, product.getId()).getStatus()).isEqualTo(ProductStatus.DELETED);
        }

    }

    @Nested
    @DisplayName("기존 사용자 삭제 경로는")
    class Describe_user_delete {

        @Test
        @DisplayName("작성자는 기존 경로로 논리 삭제할 수 있다")
        void it_allows_author_soft_delete() {
            Product product = persistProduct(ProductStatus.ONGOING);
            userService.execute(author.getId(), product.getId());
            em.flush();
            em.clear();

            assertThat(em.find(Product.class, product.getId()).getStatus()).isEqualTo(ProductStatus.DELETED);
        }

        @ParameterizedTest
        @EnumSource(MemberRole.class)
        @DisplayName("관리자를 포함해 모든 역할의 타인 삭제를 거부한다")
        void it_rejects_every_non_author_role(MemberRole role) {
            Product product = persistProduct(ProductStatus.ONGOING);
            MemberJpaEntity other = persistMember("other", role);
            assertThatThrownBy(() -> userService.execute(other.getId(), product.getId()))
                    .isInstanceOf(ForbiddenProductException.class);
            em.flush();
            em.clear();

            assertThat(em.find(Product.class, product.getId()).getStatus()).isEqualTo(ProductStatus.ONGOING);
        }

        @Test
        @DisplayName("작성자도 이미 삭제된 상품을 삭제할 수 없다")
        void it_rejects_deleted_product() {
            Product product = persistProduct(ProductStatus.DELETED);
            assertThatThrownBy(() -> userService.execute(author.getId(), product.getId()))
                    .isInstanceOf(NotFoundProductException.class);
        }

    }

    private MemberJpaEntity persistMember(String name, MemberRole role) {
        return em.persist(MemberJpaEntity.builder().name(name).nickname(name).phoneNumber(name)
                .password("test-password").role(role).status(MemberStatus.ACTIVE).build());
    }

    private Product persistProduct(ProductStatus status) {
        return em.persist(Product.builder().title("물건").description("설명").gwangsan(100)
                .member(author).status(status).type(Type.OBJECT).mode(Mode.GIVER).build());
    }
}
