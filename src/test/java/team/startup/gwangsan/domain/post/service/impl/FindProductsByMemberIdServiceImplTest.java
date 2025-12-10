package team.startup.gwangsan.domain.post.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FindProductsByMemberIdServiceImpl 단위 테스트")
class FindProductsByMemberIdServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private FindProductsByMemberIdServiceImpl service;

    private static final Long MEMBER_ID = 1L;
    private static final Type TYPE = Type.SERVICE;
    private static final Mode MODE = Mode.GIVER;

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("memberId에 해당하는 MemberDetail이 없으면 NotFoundMemberDetailException을 던진다")
        void throwsNotFoundMemberDetailException_whenMemberDetailNotFound() {
            // given
            when(memberDetailRepository.findById(MEMBER_ID))
                    .thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundMemberDetailException.class,
                    () -> service.execute(MEMBER_ID, TYPE, MODE));

            verify(memberDetailRepository).findById(MEMBER_ID);
            verifyNoInteractions(productRepository, productImageRepository);
        }

        @Test
        @DisplayName("memberId, type, mode에 해당하는 상품과 이미지 정보를 조회하여 응답 DTO 리스트를 반환한다")
        void returnsProductResponses_whenProductsExist() {
            // given
            MemberDetail memberDetail = mock(MemberDetail.class);
            Member member = mock(Member.class);
            Place place = mock(Place.class);

            when(memberDetailRepository.findById(MEMBER_ID))
                    .thenReturn(Optional.of(memberDetail));
            when(memberDetail.getMember()).thenReturn(member);
            when(member.getId()).thenReturn(MEMBER_ID);
            when(member.getNickname()).thenReturn("닉네임");
            when(memberDetail.getPlace()).thenReturn(place);
            when(place.getName()).thenReturn("광산구");
            when(memberDetail.getLight()).thenReturn(37);

            Product product1 = mock(Product.class);
            Product product2 = mock(Product.class);

            when(product1.getId()).thenReturn(100L);
            when(product1.getTitle()).thenReturn("제목1");
            when(product1.getDescription()).thenReturn("설명1");
            when(product1.getGwangsan()).thenReturn(10);
            when(product1.getType()).thenReturn(TYPE);
            when(product1.getMode()).thenReturn(MODE);

            when(product2.getId()).thenReturn(101L);
            when(product2.getTitle()).thenReturn("제목2");
            when(product2.getDescription()).thenReturn("설명2");
            when(product2.getGwangsan()).thenReturn(20);
            when(product2.getType()).thenReturn(TYPE);
            when(product2.getMode()).thenReturn(MODE);

            when(productRepository.findProductByMemberAndTypeAndModeAndStatus(member, TYPE, MODE, null))
                    .thenReturn(List.of(product1, product2));

            Image image1 = mock(Image.class);
            when(image1.getId()).thenReturn(1000L);
            when(image1.getImageUrl()).thenReturn("https://example.com/image1.png");

            Image image2 = mock(Image.class);
            when(image2.getId()).thenReturn(1001L);
            when(image2.getImageUrl()).thenReturn("https://example.com/image2.png");

            ProductImage pi1 = mock(ProductImage.class);
            when(pi1.getProduct()).thenReturn(product1);
            when(pi1.getImage()).thenReturn(image1);

            ProductImage pi2 = mock(ProductImage.class);
            when(pi2.getProduct()).thenReturn(product2);
            when(pi2.getImage()).thenReturn(image2);

            when(productImageRepository.findAllByProductIdIn(anyList()))
                    .thenReturn(List.of(pi1, pi2));

            // when
            List<GetProductResponse> responses = service.execute(MEMBER_ID, TYPE, MODE);

            // then
            assertThat(responses).hasSize(2);

            GetProductResponse r1 = responses.get(0);
            assertThat(r1.id()).isEqualTo(100L);
            assertThat(r1.title()).isEqualTo("제목1");
            assertThat(r1.content()).isEqualTo("설명1");
            assertThat(r1.gwangsan()).isEqualTo(10);
            assertThat(r1.type()).isEqualTo(TYPE);
            assertThat(r1.mode()).isEqualTo(MODE);
            assertThat(r1.member().memberId()).isEqualTo(MEMBER_ID);
            assertThat(r1.member().nickname()).isEqualTo("닉네임");
            assertThat(r1.member().placeName()).isEqualTo("광산구");
            assertThat(r1.member().light()).isEqualTo(3);
            assertThat(r1.images())
                    .extracting(GetImageResponse::imageId)
                    .containsExactly(1000L);

            GetProductResponse r2 = responses.get(1);
            assertThat(r2.id()).isEqualTo(101L);
            assertThat(r2.title()).isEqualTo("제목2");
            assertThat(r2.content()).isEqualTo("설명2");
            assertThat(r2.gwangsan()).isEqualTo(20);
            assertThat(r2.type()).isEqualTo(TYPE);
            assertThat(r2.mode()).isEqualTo(MODE);
            assertThat(r2.member().memberId()).isEqualTo(MEMBER_ID);
            assertThat(r2.images())
                    .extracting(GetImageResponse::imageId)
                    .containsExactly(1001L);

            ArgumentCaptor<List<Long>> productIdsCaptor = ArgumentCaptor.forClass(List.class);
            verify(productImageRepository).findAllByProductIdIn(productIdsCaptor.capture());
            assertThat(productIdsCaptor.getValue()).containsExactlyInAnyOrder(100L, 101L);

            verify(productRepository).findProductByMemberAndTypeAndModeAndStatus(member, TYPE, MODE, null);
        }

        @Test
        @DisplayName("light 값이 0일 때도 최소 1로 보정된 값을 반환한다")
        void returnsLightAsOne_whenRawLightIsZero() {
            // given
            MemberDetail memberDetail = mock(MemberDetail.class);
            Member member = mock(Member.class);
            Place place = mock(Place.class);

            when(memberDetailRepository.findById(MEMBER_ID))
                    .thenReturn(Optional.of(memberDetail));
            when(memberDetail.getMember()).thenReturn(member);
            when(member.getId()).thenReturn(MEMBER_ID);
            when(member.getNickname()).thenReturn("닉네임");
            when(memberDetail.getPlace()).thenReturn(place);
            when(place.getName()).thenReturn("광산구");
            when(memberDetail.getLight()).thenReturn(0); // rawLight = 0 → light = 1

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(200L);
            when(product.getTitle()).thenReturn("제목");
            when(product.getDescription()).thenReturn("설명");
            when(product.getGwangsan()).thenReturn(5);
            when(product.getType()).thenReturn(TYPE);
            when(product.getMode()).thenReturn(MODE);

            when(productRepository.findProductByMemberAndTypeAndModeAndStatus(member, TYPE, MODE, null))
                    .thenReturn(List.of(product));

            when(productImageRepository.findAllByProductIdIn(anyList()))
                    .thenReturn(List.of());

            // when
            List<GetProductResponse> responses = service.execute(MEMBER_ID, TYPE, MODE);

            // then
            assertThat(responses).hasSize(1);
            GetProductResponse response = responses.get(0);
            assertThat(response.member().light()).isEqualTo(1);
        }
    }
}