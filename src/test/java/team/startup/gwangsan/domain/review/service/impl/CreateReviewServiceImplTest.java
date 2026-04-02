package team.startup.gwangsan.domain.review.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.review.exception.AlreadyReviewedException;
import team.startup.gwangsan.domain.review.exception.CannotReviewBeforeTradeException;
import team.startup.gwangsan.domain.review.presentation.dto.request.CreateReviewRequest;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.global.util.BlockValidator;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateReviewServiceImpl 단위 테스트")
class CreateReviewServiceImplTest {

    @InjectMocks private CreateReviewServiceImpl service;

    @Mock private ReviewRepository reviewRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private ProductRepository productRepository;
    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;
    @Mock private BlockValidator blockValidator;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상 요청일 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("리뷰를 저장하고 이벤트를 발행한다")
            void it_saves_review_and_publishes_event() {
                Member reviewer = mock(Member.class);
                Member reviewed = mock(Member.class);
                when(reviewed.getId()).thenReturn(2L);

                Product product = mock(Product.class);
                when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);
                when(product.getMember()).thenReturn(reviewed);

                MemberDetail reviewedDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findById(1L)).thenReturn(Optional.of(product));
                when(reviewRepository.existsByProductAndReviewer(product, reviewer)).thenReturn(false);
                when(memberDetailRepository.findByMember(reviewed)).thenReturn(Optional.of(reviewedDetail));

                CreateReviewRequest request = new CreateReviewRequest(1L, "좋았어요", 80);
                service.execute(request);

                verify(reviewedDetail).plusLight(80);
                verify(reviewRepository).save(any());
                verify(applicationEventPublisher).publishEvent(any(Object.class));
            }
        }

        @Nested
        @DisplayName("상품이 없을 때")
        class Context_with_product_not_found {

            @Test
            @DisplayName("NotFoundProductException을 던진다")
            void it_throws_not_found_product_exception() {
                Member reviewer = mock(Member.class);
                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findById(99L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(99L, "내용", 50)))
                        .isInstanceOf(NotFoundProductException.class);
            }
        }

        @Nested
        @DisplayName("거래 완료 전 리뷰 시도 시")
        class Context_with_product_not_completed {

            @Test
            @DisplayName("CannotReviewBeforeTradeException을 던진다")
            void it_throws_cannot_review_before_trade_exception() {
                Member reviewer = mock(Member.class);
                Member owner = mock(Member.class);

                Product product = mock(Product.class);
                when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
                when(product.getMember()).thenReturn(owner);

                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findById(1L)).thenReturn(Optional.of(product));

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, "내용", 50)))
                        .isInstanceOf(CannotReviewBeforeTradeException.class);
            }
        }

        @Nested
        @DisplayName("이미 리뷰를 작성한 경우")
        class Context_with_already_reviewed {

            @Test
            @DisplayName("AlreadyReviewedException을 던진다")
            void it_throws_already_reviewed_exception() {
                Member reviewer = mock(Member.class);
                Member owner = mock(Member.class);

                Product product = mock(Product.class);
                when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);
                when(product.getMember()).thenReturn(owner);

                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findById(1L)).thenReturn(Optional.of(product));
                when(reviewRepository.existsByProductAndReviewer(product, reviewer)).thenReturn(true);

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, "내용", 50)))
                        .isInstanceOf(AlreadyReviewedException.class);
            }
        }

        @Nested
        @DisplayName("피리뷰어 MemberDetail이 없을 때")
        class Context_with_reviewed_detail_not_found {

            @Test
            @DisplayName("NotFoundMemberDetailException을 던진다")
            void it_throws_not_found_member_detail_exception() {
                Member reviewer = mock(Member.class);
                Member reviewed = mock(Member.class);

                Product product = mock(Product.class);
                when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);
                when(product.getMember()).thenReturn(reviewed);

                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findById(1L)).thenReturn(Optional.of(product));
                when(reviewRepository.existsByProductAndReviewer(product, reviewer)).thenReturn(false);
                when(memberDetailRepository.findByMember(reviewed)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, "내용", 50)))
                        .isInstanceOf(NotFoundMemberDetailException.class);
            }
        }
    }
}
