package team.startup.gwangsan.domain.trade.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TradeState 단위 테스트")
class TradeStateTest {

    private static final LocalDateTime PENDING_AT = LocalDateTime.of(2026, 1, 2, 10, 0);
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 1, 1, 9, 0);

    private static TradeRequestState pending(boolean bySeller) {
        return new TradeRequestState(TradeStatus.PENDING, bySeller, PENDING_AT);
    }

    private static TradeRequestState completed() {
        return new TradeRequestState(TradeStatus.COMPLETED, true, COMPLETED_AT);
    }

    @Nested
    @DisplayName("of() 는")
    class Describe_of {

        @Test
        @DisplayName("거래 요청이 없으면 양쪽 모두 거래를 요청할 수 있다")
        void it_allows_both_without_request() {
            TradeState state = TradeState.of(ProductPhase.ONGOING, List.of());

            assertThat(state).isEqualTo(new TradeState(false, false, null, null));
            assertThat(state.isCompletableFor(true)).isTrue();
            assertThat(state.isCompletableFor(false)).isTrue();
        }

        @Test
        @DisplayName("판매자가 완료를 요청하면 판매자 본인은 확정할 수 없고 구매자는 확정할 수 있다")
        void it_blocks_seller_after_seller_request() {
            TradeState state = TradeState.of(ProductPhase.ONGOING, List.of(pending(true)));

            assertThat(state.requestedBySeller()).isTrue();
            assertThat(state.requestedAt()).isEqualTo(PENDING_AT);
            assertThat(state.isCompletableFor(true)).isFalse();
            assertThat(state.isCompletableFor(false)).isTrue();
        }

        @Test
        @DisplayName("구매자가 완료를 요청하면 구매자 본인은 확정할 수 없고 판매자는 확정할 수 있다")
        void it_blocks_buyer_after_buyer_request() {
            TradeState state = TradeState.of(ProductPhase.ONGOING, List.of(pending(false)));

            assertThat(state.isCompletableFor(false)).isFalse();
            assertThat(state.isCompletableFor(true)).isTrue();
        }

        @Test
        @DisplayName("거래가 완료되면 양쪽 모두 확정할 수 없고 완료된 요청 시각은 남는다")
        void it_keeps_completed_request_time() {
            TradeState state = TradeState.of(ProductPhase.COMPLETED, List.of(completed()));

            assertThat(state).isEqualTo(new TradeState(true, false, null, COMPLETED_AT));
            assertThat(state.isCompletableFor(true)).isFalse();
            assertThat(state.isCompletableFor(false)).isFalse();
        }

        @Test
        @DisplayName("철회가 승인되면(ROLLED_BACK 은 조회되지 않음) 요청 흔적이 남지 않는다")
        void it_clears_after_rollback() {
            TradeState state = TradeState.of(ProductPhase.ONGOING, List.of());

            assertThat(state.requestedBySeller()).isNull();
            assertThat(state.requestedAt()).isNull();
        }

        @Test
        @DisplayName("대기 요청이 완료된 요청보다 우선한다")
        void it_prefers_pending_over_completed() {
            TradeState state = TradeState.of(ProductPhase.ONGOING, List.of(completed(), pending(false)));

            assertThat(state.requestedBySeller()).isFalse();
            assertThat(state.requestedAt()).isEqualTo(PENDING_AT);
        }

        @Test
        @DisplayName("예약 상태에서 대기 중인 요청이 있으면 예약과 요청 상태를 함께 표시한다")
        void it_shows_reservation_with_request() {
            TradeState state = TradeState.of(ProductPhase.RESERVED, List.of(pending(true)));

            assertThat(state).isEqualTo(new TradeState(false, true, true, PENDING_AT));
        }

        @Test
        @DisplayName("삭제된 상품은 완료도 예약도 아니다")
        void it_treats_deleted_as_neither() {
            TradeState state = TradeState.of(ProductPhase.DELETED, List.of());

            assertThat(state.completed()).isFalse();
            assertThat(state.reserved()).isFalse();
        }
    }

    @Test
    @DisplayName("isCompletableFor() 는 거래가 완료되면 대기 요청 정보와 무관하게 false 다")
    void it_is_never_completable_after_completion() {
        TradeState state = new TradeState(true, false, false, PENDING_AT);

        assertThat(state.isCompletableFor(true)).isFalse();
        assertThat(state.isCompletableFor(false)).isFalse();
    }
}
