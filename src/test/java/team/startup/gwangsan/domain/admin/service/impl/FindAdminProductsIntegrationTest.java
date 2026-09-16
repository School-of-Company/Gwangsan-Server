package team.startup.gwangsan.domain.admin.service.impl;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import({QueryDslConfig.class, FindAdminProductsServiceImpl.class})
@DisplayName("관리자 상품 목록 영속성 통합 테스트")
class FindAdminProductsIntegrationTest {
    @Autowired private TestEntityManager em;
    @Autowired private FindAdminProductsServiceImpl service;

    @Nested
    @DisplayName("목록 응답은")
    class Describe_listing {
        @Test
        @DisplayName("다른 지점의 게시글과 여러 이미지를 3개 쿼리로 조회한다")
        void it_lists_different_places_without_n_plus_one() {
            Dong dong = em.persist(Dong.builder().name("테스트동").build());
            for (int i = 0; i < 3; i++) {
                Member member = em.persist(Member.builder().name("회원" + i).nickname("회원" + i)
                        .phoneNumber("0100000000" + i).password("password")
                        .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build());
                Place place = em.persist(Place.builder().name("지점" + i).build());
                em.persist(MemberDetail.builder().member(member).dong(dong).place(place)
                        .gwangsan(10000).light(30).description("소개").build());
                Product product = em.persist(Product.builder().member(member).title("게시글" + i)
                        .description("설명").gwangsan(5000).type(Type.OBJECT).mode(Mode.GIVER)
                        .status(ProductStatus.ONGOING).build());
                for (int imageIndex = 0; imageIndex < 2; imageIndex++) {
                    Image image = em.persist(Image.builder().imageUrl("test/" + i + "/" + imageIndex).build());
                    em.persist(ProductImage.builder().product(product).image(image).build());
                }
            }
            em.flush();
            em.clear();
            var statistics = em.getEntityManager().getEntityManagerFactory()
                    .unwrap(SessionFactory.class).getStatistics();
            statistics.clear();

            var result = service.execute(null, null, null, 20);

            assertThat(result).extracting(response -> response.member().placeName())
                    .containsExactly("지점2", "지점1", "지점0");
            assertThat(result).extracting(GetProductResponse::images).allSatisfy(images -> assertThat(images).hasSize(2));
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        }
    }
}
