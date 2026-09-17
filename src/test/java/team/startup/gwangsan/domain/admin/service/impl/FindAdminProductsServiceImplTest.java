package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAdminProductsServiceImpl 단위 테스트")
class FindAdminProductsServiceImplTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @InjectMocks
    private FindAdminProductsServiceImpl service;

    private static final Type TYPE = Type.SERVICE;
    private static final Mode MODE = Mode.GIVER;

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("다른 지점 회원과 이미지를 일괄 조회하여 DTO로 반환한다")
        void it_maps_members_from_different_places_and_images() {
            // given
            Product product1 = mock(Product.class);
            Product product2 = mock(Product.class);

            Member member1 = mock(Member.class);
            Member member2 = mock(Member.class);

            when(member1.getId()).thenReturn(1L);
            when(member2.getId()).thenReturn(2L);

            when(product1.getId()).thenReturn(100L);
            when(product1.getTitle()).thenReturn("상품1");
            when(product1.getDescription()).thenReturn("설명1");
            when(product1.getGwangsan()).thenReturn(10);
            when(product1.getType()).thenReturn(TYPE);
            when(product1.getMode()).thenReturn(MODE);
            when(product1.getMember()).thenReturn(member1);
            when(product1.getStatus()).thenReturn(ProductStatus.ONGOING);

            when(product2.getId()).thenReturn(101L);
            when(product2.getTitle()).thenReturn("상품2");
            when(product2.getDescription()).thenReturn("설명2");
            when(product2.getGwangsan()).thenReturn(20);
            when(product2.getType()).thenReturn(TYPE);
            when(product2.getMode()).thenReturn(MODE);
            when(product2.getMember()).thenReturn(member2);
            when(product2.getStatus()).thenReturn(ProductStatus.COMPLETED);

            when(productRepository.findAdminProducts(TYPE, MODE, 102L, 20)).thenReturn(List.of(product1, product2));

            MemberDetail detail1 = mock(MemberDetail.class);
            MemberDetail detail2 = mock(MemberDetail.class);
            Place place1 = mock(Place.class);
            Place place2 = mock(Place.class);

            when(detail1.getMember()).thenReturn(member1);
            when(detail2.getMember()).thenReturn(member2);
            when(detail1.getLight()).thenReturn(35);
            when(detail2.getLight()).thenReturn(0);
            when(detail1.getPlace()).thenReturn(place1);
            when(detail2.getPlace()).thenReturn(place2);
            when(place1.getName()).thenReturn("광산구");
            when(place2.getName()).thenReturn("다른 지점");

            when(memberDetailRepository.findAllByMemberIdIn(anyList()))
                    .thenReturn(List.of(detail1, detail2));

            Image img1 = mock(Image.class);
            when(img1.getId()).thenReturn(1000L);
            when(img1.getImageUrl()).thenReturn("url1");

            Image img2 = mock(Image.class);
            when(img2.getId()).thenReturn(1001L);
            when(img2.getImageUrl()).thenReturn("url2");

            ProductImage pi1 = mock(ProductImage.class);
            when(pi1.getProduct()).thenReturn(product1);
            when(pi1.getImage()).thenReturn(img1);

            ProductImage pi2 = mock(ProductImage.class);
            when(pi2.getProduct()).thenReturn(product2);
            when(pi2.getImage()).thenReturn(img2);

            when(productImageRepository.findAllByProductIdIn(anyList()))
                    .thenReturn(List.of(pi1, pi2));

            // when
            List<GetProductResponse> result = service.execute(TYPE, MODE, 102L, 20);

            // then
            assertThat(result).hasSize(2);

            GetProductResponse r1 = result.get(0);
            assertThat(r1.id()).isEqualTo(100L);
            assertThat(r1.member().placeName()).isEqualTo("광산구");
            assertThat(r1.member().light()).isEqualTo(3);
            assertThat(r1.images().get(0).imageId()).isEqualTo(1000L);
            assertThat(r1.isCompleted()).isFalse();
            assertThat(r1.isReserved()).isFalse();

            GetProductResponse r2 = result.get(1);
            assertThat(r2.id()).isEqualTo(101L);
            assertThat(r2.member().placeName()).isEqualTo("다른 지점");
            assertThat(r2.member().light()).isEqualTo(1);
            assertThat(r2.images().get(0).imageId()).isEqualTo(1001L);
            assertThat(r2.isCompleted()).isTrue();
            assertThat(r2.isReserved()).isFalse();

            verify(productRepository).findAdminProducts(TYPE, MODE, 102L, 20);
            verify(memberDetailRepository).findAllByMemberIdIn(anyList());
            verify(productImageRepository).findAllByProductIdIn(anyList());
        }

        @Test
        @DisplayName("작성자의 MemberDetail이 없으면 기본값(light=1, placeName=null)으로 채워 반환한다")
        void it_fills_default_when_member_detail_is_missing() {
            // given
            Product product = mock(Product.class);
            Member member = mock(Member.class);

            when(member.getId()).thenReturn(1L);

            when(product.getId()).thenReturn(100L);
            when(product.getTitle()).thenReturn("상품1");
            when(product.getDescription()).thenReturn("설명1");
            when(product.getGwangsan()).thenReturn(10);
            when(product.getType()).thenReturn(TYPE);
            when(product.getMode()).thenReturn(MODE);
            when(product.getMember()).thenReturn(member);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);

            when(productRepository.findAdminProducts(TYPE, MODE, 102L, 20)).thenReturn(List.of(product));

            when(memberDetailRepository.findAllByMemberIdIn(anyList())).thenReturn(List.of());
            when(productImageRepository.findAllByProductIdIn(anyList())).thenReturn(List.of());

            // when
            List<GetProductResponse> result = service.execute(TYPE, MODE, 102L, 20);

            // then
            assertThat(result).hasSize(1);
            GetProductResponse r = result.get(0);
            assertThat(r.member().light()).isEqualTo(1);
            assertThat(r.member().placeName()).isNull();
        }

        @Test
        @DisplayName("조회된 상품이 0개이면 빈 리스트를 반환한다")
        void it_returns_empty_without_secondary_queries() {
            // given
            when(productRepository.findAdminProducts(TYPE, MODE, 102L, 20)).thenReturn(List.of());

            // when
            List<GetProductResponse> result = service.execute(TYPE, MODE, 102L, 20);

            // then
            assertThat(result).isEmpty();

            verify(productImageRepository, never()).findAllByProductIdIn(anyList());
            verify(memberDetailRepository, never()).findAllByMemberIdIn(anyList());
        }
    }
}