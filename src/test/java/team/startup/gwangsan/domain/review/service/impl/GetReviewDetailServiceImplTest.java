package team.startup.gwangsan.domain.review.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewDetailResponse;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetReviewDetailServiceImpl 단위 테스트")
class GetReviewDetailServiceImplTest {

    @InjectMocks private GetReviewDetailServiceImpl service;

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductImageRepository productImageRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("리뷰가 없을 때")
        class Context_with_review_not_found {

            @Test
            @DisplayName("NotFoundReviewException을 던진다")
            void it_throws_not_found_review_exception() {
                when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(99L))
                        .isInstanceOf(NotFoundReviewException.class);
            }
        }

        @Nested
        @DisplayName("리뷰가 있을 때")
        class Context_with_valid_review {

            @Test
            @DisplayName("ReviewDetailResponse를 반환한다")
            void it_returns_review_detail_response() {
                Product product = mock(Product.class);
                when(product.getId()).thenReturn(100L);
                when(product.getTitle()).thenReturn("상품명");

                Review review = mock(Review.class);
                when(review.getId()).thenReturn(1L);
                when(review.getProduct()).thenReturn(product);
                when(review.getContent()).thenReturn("좋았어요");
                when(review.getLight()).thenReturn(8);

                ProductImage pi = mock(ProductImage.class);
                Image image = mock(Image.class);
                when(image.getId()).thenReturn(200L);
                when(image.getImageUrl()).thenReturn("https://img.url/4.jpg");
                when(pi.getImage()).thenReturn(image);

                when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
                when(productImageRepository.findAllByProductId(100L)).thenReturn(List.of(pi));

                ReviewDetailResponse response = service.execute(1L);

                assertThat(response.reviewId()).isEqualTo(1L);
                assertThat(response.title()).isEqualTo("상품명");
                assertThat(response.content()).isEqualTo("좋았어요");
                assertThat(response.light()).isEqualTo(8);
                assertThat(response.imageUrls()).hasSize(1);
            }
        }
    }
}
