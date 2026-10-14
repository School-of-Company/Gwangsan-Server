package team.startup.gwangsan.domain.review.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.domain.review.exception.CannotReviewSelfException;
import team.startup.gwangsan.domain.review.exception.NotTradeParticipantException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("후기 순수 규칙 단위 테스트")
class ReviewRulesTest {

    @Test
    @DisplayName("거래 양쪽 방향의 후기만 허용한다")
    void it_allows_both_directions_only() {
        assertThatCode(() -> Review.validateParticipants(1L, 2L, 1L, 2L)).doesNotThrowAnyException();
        assertThatCode(() -> Review.validateParticipants(2L, 1L, 1L, 2L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> Review.validateParticipants(1L, 3L, 1L, 2L)).isInstanceOf(NotTradeParticipantException.class);
    }

    @Test
    @DisplayName("본인 후기를 거절한다")
    void it_rejects_self() {
        assertThatThrownBy(() -> Review.validateNotSelf(1L, 1L)).isInstanceOf(CannotReviewSelfException.class);
    }
}
