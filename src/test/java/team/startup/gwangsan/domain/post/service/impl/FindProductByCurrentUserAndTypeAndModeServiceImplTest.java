package team.startup.gwangsan.domain.post.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindProductByCurrentUserAndTypeAndModeServiceImplTest {

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MemberUtil memberUtil;

    @InjectMocks
    private FindProductByCurrentUserAndTypeAndModeServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("MemberDetail이 존재하지 않으면 NotFoundMemberDetailException 발생")
        void it_throws_NotFoundMemberDetailException_when_memberDetail_not_found() {
            // given
            Member member = mock(Member.class);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(member.getId()).thenReturn(10L);
            when(memberDetailRepository.findById(10L)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundMemberDetailException.class,
                    () -> service.execute(Type.SERVICE, Mode.GIVER));

            verify(memberUtil).getCurrentMember();
            verify(memberDetailRepository).findById(10L);
        }

        @Test
        @DisplayName("정상적으로 Product 리스트와 이미지 리스트를 매핑하여 반환한다")
        void it_returns_GetProductResponse_list_properly() {
            // given
            Member member = mock(Member.class);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(member.getId()).thenReturn(1L);
            when(member.getNickname()).thenReturn("홍길동");
            MemberDetail memberDetail = mock(MemberDetail.class);
            when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(memberDetail));
            when(memberDetail.getLight()).thenReturn(37);

            Place place = Place.builder()
                    .name("광산구")
                    .build();
            when(memberDetail.getPlace()).thenReturn(place);

            Product product1 = mock(Product.class);
            Product product2 = mock(Product.class);

            when(product1.getId()).thenReturn(101L);
            when(product1.getTitle()).thenReturn("상품1");
            when(product1.getDescription()).thenReturn("설명1");
            when(product1.getGwangsan()).thenReturn(3);
            when(product1.getType()).thenReturn(Type.SERVICE);
            when(product1.getMode()).thenReturn(Mode.GIVER);

            when(product2.getId()).thenReturn(102L);
            when(product2.getTitle()).thenReturn("상품2");
            when(product2.getDescription()).thenReturn("설명2");
            when(product2.getGwangsan()).thenReturn(5);
            when(product2.getType()).thenReturn(Type.SERVICE);
            when(product2.getMode()).thenReturn(Mode.GIVER);

            when(productRepository.findProductByMemberAndTypeAndModeAndStatus(
                    member, Type.SERVICE, Mode.GIVER, ProductStatus.ONGOING
            )).thenReturn(List.of(product1, product2));

            Image img1 = mock(Image.class);
            Image img2 = mock(Image.class);

            when(img1.getId()).thenReturn(201L);
            when(img1.getImageUrl()).thenReturn("url1");

            when(img2.getId()).thenReturn(202L);
            when(img2.getImageUrl()).thenReturn("url2");

            ProductImage pi1 = mock(ProductImage.class);
            ProductImage pi2 = mock(ProductImage.class);

            when(pi1.getProduct()).thenReturn(product1);
            when(pi1.getImage()).thenReturn(img1);

            when(pi2.getProduct()).thenReturn(product2);
            when(pi2.getImage()).thenReturn(img2);

            when(productImageRepository.findAllByProductIdIn(List.of(101L, 102L)))
                    .thenReturn(List.of(pi1, pi2));

            // when
            var result = service.execute(Type.SERVICE, Mode.GIVER);

            // then
            assertThat(result).hasSize(2);

            assertThat(result.get(0).id()).isEqualTo(101L);
            assertThat(result.get(0).images()).containsExactly(
                    new GetImageResponse(201L, "url1")
            );

            assertThat(result.get(1).id()).isEqualTo(102L);
            assertThat(result.get(1).images()).containsExactly(
                    new GetImageResponse(202L, "url2")
            );

            assertThat(result.get(0).member().light()).isEqualTo(3);

            verify(memberUtil).getCurrentMember();
            verify(memberDetailRepository).findById(1L);
            verify(productRepository).findProductByMemberAndTypeAndModeAndStatus(member, Type.SERVICE, Mode.GIVER, ProductStatus.ONGOING);
            verify(productImageRepository).findAllByProductIdIn(List.of(101L, 102L));
        }
    }
}