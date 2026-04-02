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
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.review.repository.projection.MyReviewDto;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMyReviewListServiceImpl 단위 테스트")
class GetMyReviewListServiceImplTest {

    @InjectMocks private GetMyReviewListServiceImpl service;

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private MemberUtil memberUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("리뷰가 없을 때")
        class Context_with_no_reviews {

            @Test
            @DisplayName("빈 리스트를 반환한다")
            void it_returns_empty_list() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(1L);
                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(reviewRepository.findMyReviews(1L)).thenReturn(List.of());

                List<ReviewResponse> result = service.execute();

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
                when(member.getId()).thenReturn(1L);
                when(member.getNickname()).thenReturn("리뷰어");
                when(memberUtil.getCurrentMember()).thenReturn(member);

                MyReviewDto dto = new MyReviewDto(10L, 100L, "좋았어요", 8);
                when(reviewRepository.findMyReviews(1L)).thenReturn(List.of(dto));

                ProductImage pi = mock(ProductImage.class);
                Product product = mock(Product.class);
                Image image = mock(Image.class);
                when(product.getId()).thenReturn(100L);
                when(image.getId()).thenReturn(200L);
                when(image.getImageUrl()).thenReturn("https://img.url/1.jpg");
                when(pi.getProduct()).thenReturn(product);
                when(pi.getImage()).thenReturn(image);

                when(productImageRepository.findAllByProductIdIn(List.of(100L))).thenReturn(List.of(pi));

                List<ReviewResponse> result = service.execute();

                assertThat(result).hasSize(1);
                assertThat(result.get(0).reviewId()).isEqualTo(10L);
                assertThat(result.get(0).content()).isEqualTo("좋았어요");
                assertThat(result.get(0).reviewerName()).isEqualTo("리뷰어");
                assertThat(result.get(0).imageUrls()).hasSize(1);
            }
        }
    }
}
