package team.startup.gwangsan.adapter.integration.admin;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.adapter.persistence.post.ProductImage;
import team.startup.gwangsan.adapter.persistence.post.ProductImagePersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.post.ProductPersistenceAdapter;
import team.startup.gwangsan.application.admin.data.AdminProduct;
import team.startup.gwangsan.application.admin.data.AdminProductSearch;
import team.startup.gwangsan.application.post.service.ProductPublicQueryService;
import team.startup.gwangsan.application.post.service.ProductStatusService;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import({QueryDslConfig.class, AdminProductBridge.class, ProductPublicQueryService.class, ProductStatusService.class,
        ProductPersistenceAdapter.class, ProductImagePersistenceAdapter.class})
@DisplayName("관리자 상품 목록 영속성 통합 테스트")
class AdminProductBridgeIntegrationTest {
    @Autowired private TestEntityManager em;
    @Autowired private AdminProductBridge bridge;

    private DongJpaEntity dong;

    @BeforeEach
    void setUp() {
        dong = em.persist(DongJpaEntity.builder().name("테스트동").build());
    }

    @Nested
    @DisplayName("목록 응답은")
    class Describe_listing {
        @Test
        @DisplayName("it_다른 지점의 게시글과 여러 이미지를 3개 쿼리로 조회한다")
        void it_lists_different_places_without_n_plus_one() {
            for (int i = 0; i < 3; i++) {
                MemberJpaEntity member = member("회원" + i);
                Place place = em.persist(Place.builder().name("지점" + i).build());
                detail(member, place, 30);
                Product product = product(member, "게시글" + i, ProductStatus.ONGOING);
                for (int imageIndex = 0; imageIndex < 2; imageIndex++) {
                    Image image = em.persist(Image.builder().imageUrl("test/" + i + "/" + imageIndex).build());
                    em.persist(ProductImage.builder().product(product).image(image).build());
                }
            }
            Statistics statistics = clearAndStatistics();

            List<AdminProduct> result = bridge.findProducts(new AdminProductSearch(null, null, null, 20));

            assertThat(result).extracting(response -> response.member().placeName())
                    .containsExactly("지점2", "지점1", "지점0");
            assertThat(result).extracting(response -> response.member().nickname())
                    .containsExactly("회원2", "회원1", "회원0");
            assertThat(result).extracting(response -> response.member().light()).containsOnly(3);
            assertThat(result).extracting(AdminProduct::images).allSatisfy(images -> assertThat(images).hasSize(2));
            assertThat(result).extracting(AdminProduct::type, AdminProduct::mode, AdminProduct::content)
                    .containsOnly(org.assertj.core.groups.Tuple.tuple("OBJECT", "GIVER", "설명"));
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("it_작성자 상세가 없으면 닉네임은 그대로, light=1·placeName=null 로 채운다")
        void it_fills_default_when_member_detail_is_missing() {
            product(member("상세없음"), "게시글", ProductStatus.COMPLETED);
            clearAndStatistics();

            AdminProduct product = bridge.findProducts(new AdminProductSearch(null, null, null, 20)).getFirst();

            assertThat(product.member().nickname()).isEqualTo("상세없음");
            assertThat(product.member().light()).isEqualTo(1);
            assertThat(product.member().placeName()).isNull();
            assertThat(product.completed()).isTrue();
            assertThat(product.reserved()).isFalse();
            assertThat(product.images()).isEmpty();
        }

        @Test
        @DisplayName("it_빛이 10 미만인 작성자의 예약 상품은 light=1 과 예약 상태로 반환한다 (상세 place_id 는 NOT NULL)")
        void it_maps_reservation_with_minimum_light() {
            MemberJpaEntity member = member("빛낮음");
            detail(member, em.persist(Place.builder().name("지점").build()), 5);
            product(member, "예약", ProductStatus.RESERVATION);
            clearAndStatistics();

            AdminProduct product = bridge.findProducts(new AdminProductSearch(null, null, null, 20)).getFirst();

            assertThat(product.member().placeName()).isEqualTo("지점");
            assertThat(product.member().light()).isEqualTo(1);
            assertThat(product.reserved()).isTrue();
            assertThat(product.completed()).isFalse();
        }

        @Test
        @DisplayName("it_조회된 상품이 없으면 추가 쿼리 없이 빈 목록을 반환한다")
        void it_returns_empty_without_secondary_queries() {
            Statistics statistics = clearAndStatistics();

            assertThat(bridge.findProducts(new AdminProductSearch("OBJECT", "RECEIVER", 5L, 10))).isEmpty();
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        }
    }

    private MemberJpaEntity member(String nickname) {
        return em.persist(MemberJpaEntity.builder().name(nickname).nickname(nickname).phoneNumber(nickname)
                .password("password").role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build());
    }

    private void detail(MemberJpaEntity member, Place place, int light) {
        em.persist(MemberDetailJpaEntity.builder().member(member).dong(dong).place(place)
                .gwangsan(10000).light(light).description("소개").build());
    }

    private Product product(MemberJpaEntity member, String title, ProductStatus status) {
        return em.persist(Product.builder().member(member).title(title).description("설명").gwangsan(5000)
                .type(Type.OBJECT).mode(Mode.GIVER).status(status).build());
    }

    private Statistics clearAndStatistics() {
        em.flush();
        em.clear();
        Statistics statistics = em.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
