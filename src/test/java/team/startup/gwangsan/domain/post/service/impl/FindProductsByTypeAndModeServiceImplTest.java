package team.startup.gwangsan.domain.post.service.impl;

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
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindProductsByTypeAndModeServiceImpl 단위 테스트")
class FindProductsByTypeAndModeServiceImplTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @Mock
    private MemberUtil memberUtil;

    @InjectMocks
    private FindProductsByTypeAndModeServiceImpl service;

    private static final Type TYPE = Type.SERVICE;
    private static final Mode MODE = Mode.GIVER;

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("현재 사용자 동네와 동일한 동네의 상품을 조회하고 DTO 리스트를 반환한다")
        void execute() {
            // given
            Member me = mock(Member.class);
            when(me.getId()).thenReturn(99L);
            when(memberUtil.getCurrentMember()).thenReturn(me);

            Place myPlace = mock(Place.class);
            when(memberDetailRepository.findPlaceByMemberId(me.getId())).thenReturn(myPlace);

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

            when(product2.getId()).thenReturn(101L);
            when(product2.getTitle()).thenReturn("상품2");
            when(product2.getDescription()).thenReturn("설명2");
            when(product2.getGwangsan()).thenReturn(20);
            when(product2.getType()).thenReturn(TYPE);
            when(product2.getMode()).thenReturn(MODE);
            when(product2.getMember()).thenReturn(member2);

            when(productRepository.findProductsByTypeAndModeAndMemberDetailPlaceAndStatus(
                    TYPE, MODE, myPlace, ProductStatus.ONGOING
            )).thenReturn(List.of(product1, product2));

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
            when(place2.getName()).thenReturn("광산구");

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
            List<GetProductResponse> result = service.execute(TYPE, MODE);

            // then
            assertThat(result).hasSize(2);

            GetProductResponse r1 = result.get(0);
            assertThat(r1.id()).isEqualTo(100L);
            assertThat(r1.member().light()).isEqualTo(3);
            assertThat(r1.images().get(0).imageId()).isEqualTo(1000L);

            GetProductResponse r2 = result.get(1);
            assertThat(r2.id()).isEqualTo(101L);
            assertThat(r2.member().light()).isEqualTo(1);
            assertThat(r2.images().get(0).imageId()).isEqualTo(1001L);

            verify(productRepository).findProductsByTypeAndModeAndMemberDetailPlaceAndStatus(
                    TYPE, MODE, myPlace, ProductStatus.ONGOING
            );
            verify(memberDetailRepository).findAllByMemberIdIn(anyList());
            verify(productImageRepository).findAllByProductIdIn(anyList());
        }

        @Test
        @DisplayName("조회된 상품이 0개이면 빈 리스트를 반환한다")
        void execute_returnsEmptyList_whenNoProducts() {
            // given
            Member me = mock(Member.class);
            when(me.getId()).thenReturn(99L);
            when(memberUtil.getCurrentMember()).thenReturn(me);

            Place myPlace = mock(Place.class);
            when(memberDetailRepository.findPlaceByMemberId(me.getId())).thenReturn(myPlace);

            when(productRepository.findProductsByTypeAndModeAndMemberDetailPlaceAndStatus(
                    TYPE, MODE, myPlace, ProductStatus.ONGOING
            )).thenReturn(List.of());

            // when
            List<GetProductResponse> result = service.execute(TYPE, MODE);

            // then
            assertThat(result).isEmpty();

            verify(productImageRepository, never()).findAllByProductIdIn(anyList());
            verify(memberDetailRepository, never()).findAllByMemberIdIn(anyList());
        }
    }
}