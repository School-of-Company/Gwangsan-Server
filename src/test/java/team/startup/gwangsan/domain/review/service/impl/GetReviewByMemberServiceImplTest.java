package team.startup.gwangsan.domain.review.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.review.repository.projection.ReceivedReviewDto;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetReviewByMemberServiceImpl 단위 테스트")
class GetReviewByMemberServiceImplTest {

    @InjectMocks private GetReviewByMemberServiceImpl service;

    @Mock private MemberRepository memberRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductImageRepository productImageRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("회원이 없을 때")
        class Context_with_member_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                when(memberRepository.findById(99L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(99L))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }

        @Nested
        @DisplayName("리뷰가 없을 때")
        class Context_with_no_reviews {

            @Test
            @DisplayName("빈 리스트를 반환한다")
            void it_returns_empty_list() {
                Member member = mock(Member.class);
                when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
                when(reviewRepository.findReceivedReviews(1L)).thenReturn(List.of());

                List<ReviewResponse> result = service.execute(1L);

                assertThat(result).isEmpty();
                verifyNoInteractions(productImageRepository);
            }
        }

        @Nested
        @DisplayName("리뷰가 있을 때")
        class Context_with_reviews {

            @Test
            @DisplayName("이미지와 함께 ReviewResponse 리스트를 반환한다")
            void it_returns_review_response_list() {
                Member member = mock(Member.class);
                when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

                ReceivedReviewDto dto = new ReceivedReviewDto(30L, 200L, "최고예요", 10, "작성자닉");
                when(reviewRepository.findReceivedReviews(1L)).thenReturn(List.of(dto));

                ProductImage pi = mock(ProductImage.class);
                Product product = mock(Product.class);
                Image image = mock(Image.class);
                when(product.getId()).thenReturn(200L);
                when(image.getId()).thenReturn(400L);
                when(image.getImageUrl()).thenReturn("https://img.url/3.jpg");
                when(pi.getProduct()).thenReturn(product);
                when(pi.getImage()).thenReturn(image);

                when(productImageRepository.findAllByProductIdIn(List.of(200L))).thenReturn(List.of(pi));

                List<ReviewResponse> result = service.execute(1L);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).reviewId()).isEqualTo(30L);
                assertThat(result.get(0).reviewerName()).isEqualTo("작성자닉");
                assertThat(result.get(0).imageUrls()).hasSize(1);
            }
        }
    }
}
