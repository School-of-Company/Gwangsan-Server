package team.startup.gwangsan.application.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.review.data.CreateReviewCommand;
import team.startup.gwangsan.application.review.data.ReviewTrade;
import team.startup.gwangsan.application.review.port.out.*;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.review.exception.*;
import team.startup.gwangsan.domain.review.model.Review;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateReviewService 단위 테스트")
class CreateReviewServiceTest {

    private static final long PRODUCT = 100L;
    private static final long BUYER = 1L;
    private static final long SELLER = 2L;
    private static final long OUTSIDER = 9L;

    @InjectMocks private CreateReviewService service;

    @Mock private ReviewPort reviewPort;
    @Mock private ReviewProductPort reviewProductPort;
    @Mock private ReviewMemberPort reviewMemberPort;
    @Mock private ReviewBlockPort reviewBlockPort;
    @Mock private ReviewTradePort reviewTradePort;
    @Mock private ReviewAlertPort reviewAlertPort;

    private void execute(long reviewerId, long reviewedId) {
        service.execute(new CreateReviewCommand(reviewerId, PRODUCT, reviewedId, "친절했습니다", 30));
    }

    private void givenCompletedTrade() {
        when(reviewProductPort.isCompleted(PRODUCT)).thenReturn(true);
        when(reviewTradePort.findCompletedTrade(PRODUCT)).thenReturn(Optional.of(new ReviewTrade(BUYER, SELLER)));
    }

    private void assertNoSideEffects() {
        verify(reviewPort, never()).save(any());
        verify(reviewMemberPort, never()).addLight(anyLong(), any());
        verifyNoInteractions(reviewAlertPort);
    }

    @Nested
    @DisplayName("정상 요청일 때")
    class Describe_success {

        @Test
        @DisplayName("구매자가 판매자를 리뷰하면 저장 후 대상의 점수를 올리고 알림을 요청한다")
        void it_saves_review() {
            givenCompletedTrade();
            when(reviewPort.save(any())).thenReturn(55L);

            execute(BUYER, SELLER);

            ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
            verify(reviewPort).save(saved.capture());
            assertThat(saved.getValue()).isEqualTo(new Review(null, PRODUCT, BUYER, SELLER, "친절했습니다", 30));
            verify(reviewBlockPort).validateNotBlocked(BUYER, SELLER);
            verify(reviewMemberPort).addLight(SELLER, 30);
            verify(reviewAlertPort).notifyReviewed(55L, SELLER);
        }

        @Test
        @DisplayName("판매자가 구매자를 리뷰하면 구매자의 점수만 올린다")
        void it_raises_only_reviewed_light() {
            givenCompletedTrade();
            when(reviewPort.save(any())).thenReturn(56L);

            execute(SELLER, BUYER);

            verify(reviewMemberPort).addLight(BUYER, 30);
            verify(reviewMemberPort, never()).addLight(eq(SELLER), any());
        }
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"1, 9", "2, 9", "9, 1"})
    @DisplayName("거래 당사자 한 쌍이 아니면 NotTradeParticipantException 을 던지고 부작용이 없다")
    void it_requires_participants(long reviewerId, long reviewedId) {
        givenCompletedTrade();

        assertThatThrownBy(() -> execute(reviewerId, reviewedId)).isInstanceOf(NotTradeParticipantException.class);
        assertNoSideEffects();
    }

    @Test
    @DisplayName("본인을 리뷰하면 CannotReviewSelfException 을 던진다")
    void it_rejects_self() {
        assertThatThrownBy(() -> execute(BUYER, BUYER)).isInstanceOf(CannotReviewSelfException.class);
        verifyNoInteractions(reviewProductPort);
    }

    @Test
    @DisplayName("상품이 없으면 NotFoundProductException 을 던진다")
    void it_requires_product() {
        when(reviewProductPort.isCompleted(PRODUCT)).thenThrow(new NotFoundProductException());

        assertThatThrownBy(() -> execute(BUYER, SELLER)).isInstanceOf(NotFoundProductException.class);
    }

    @Test
    @DisplayName("거래 완료 전이면 CannotReviewBeforeTradeException 을 던진다")
    void it_requires_completed_product() {
        when(reviewProductPort.isCompleted(PRODUCT)).thenReturn(false);

        assertThatThrownBy(() -> execute(BUYER, SELLER)).isInstanceOf(CannotReviewBeforeTradeException.class);
        verifyNoInteractions(reviewMemberPort, reviewTradePort);
    }

    @Test
    @DisplayName("리뷰 대상 회원이 없으면 NotFoundMemberException 을 던지고 차단 검사 전에 멈춘다")
    void it_requires_reviewed_member() {
        when(reviewProductPort.isCompleted(PRODUCT)).thenReturn(true);
        when(reviewMemberPort.findNickname(SELLER)).thenThrow(new NotFoundMemberException());

        assertThatThrownBy(() -> execute(BUYER, SELLER)).isInstanceOf(NotFoundMemberException.class);
        verifyNoInteractions(reviewBlockPort);
    }

    @Test
    @DisplayName("완료된 거래 내역이 없으면 CannotReviewBeforeTradeException 을 던진다")
    void it_requires_completed_trade() {
        when(reviewProductPort.isCompleted(PRODUCT)).thenReturn(true);
        when(reviewTradePort.findCompletedTrade(PRODUCT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> execute(BUYER, SELLER)).isInstanceOf(CannotReviewBeforeTradeException.class);
    }

    @Test
    @DisplayName("이미 리뷰했으면 AlreadyReviewedException 을 던진다")
    void it_rejects_duplicate() {
        givenCompletedTrade();
        when(reviewPort.exists(PRODUCT, BUYER)).thenReturn(true);

        assertThatThrownBy(() -> execute(BUYER, SELLER)).isInstanceOf(AlreadyReviewedException.class);
        assertNoSideEffects();
    }

    @Test
    @DisplayName("대상 회원 상세가 없으면 NotFoundMemberDetailException 을 던지고 알림을 요청하지 않는다")
    void it_propagates_missing_member_detail() {
        givenCompletedTrade();
        when(reviewPort.save(any())).thenReturn(55L);
        doThrow(new NotFoundMemberDetailException()).when(reviewMemberPort).addLight(SELLER, 30);

        assertThatThrownBy(() -> execute(BUYER, SELLER)).isInstanceOf(NotFoundMemberDetailException.class);
        verifyNoInteractions(reviewAlertPort);
    }
}
