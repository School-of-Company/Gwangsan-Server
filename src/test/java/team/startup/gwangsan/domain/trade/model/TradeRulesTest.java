package team.startup.gwangsan.domain.trade.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.domain.trade.exception.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("거래 순수 규칙 단위 테스트")
class TradeRulesTest {

    private static final long OWNER = 1L;
    private static final long OTHER = 2L;

    @Nested
    @DisplayName("TradeProduct.sides() 는")
    class Describe_sides {

        @Test
        @DisplayName("GIVER 글이면 작성자가 판매자, 상대가 구매자다")
        void it_makes_owner_seller_for_giver() {
            TradeProduct product = new TradeProduct(10L, OWNER, true, ProductPhase.ONGOING, 100);

            assertThat(product.sides(OWNER, OTHER)).isEqualTo(new TradeSides(OTHER, OWNER, false));
            assertThat(product.sides(OTHER, OWNER)).isEqualTo(new TradeSides(OTHER, OWNER, true));
        }

        @Test
        @DisplayName("RECEIVER 글이면 작성자가 구매자다")
        void it_makes_owner_buyer_for_receiver() {
            TradeProduct product = new TradeProduct(10L, OWNER, false, ProductPhase.ONGOING, 100);

            assertThat(product.sides(OWNER, OTHER)).isEqualTo(new TradeSides(OWNER, OTHER, true));
        }

        @Test
        @DisplayName("작성자만 isOwnedBy 다")
        void it_identifies_author() {
            TradeProduct product = new TradeProduct(10L, OWNER, true, ProductPhase.ONGOING, 100);

            assertThat(product.isOwnedBy(OWNER)).isTrue();
            assertThat(product.isOwnedBy(OTHER)).isFalse();
        }

        @Test
        @DisplayName("완료된 상품이면 TradeAlreadyCompleteException 을 던진다")
        void it_rejects_completed_product() {
            TradeProduct product = new TradeProduct(10L, OWNER, true, ProductPhase.COMPLETED, 100);

            assertThatThrownBy(product::validateNotCompleted).isInstanceOf(TradeAlreadyCompleteException.class);
        }
    }

    @Nested
    @DisplayName("TradeComplete 는")
    class Describe_trade_complete {

        @Test
        @DisplayName("요청한 쪽 역할을 requestedBySeller 로 남긴다")
        void it_records_requester_role() {
            TradeComplete bySeller = TradeComplete.request(10L, new TradeSides(OTHER, OWNER, false));

            assertThat(bySeller.status()).isEqualTo(TradeStatus.PENDING);
            assertThat(bySeller.requestedBySeller()).isTrue();
            assertThat(bySeller.buyerId()).isEqualTo(OTHER);
            assertThat(bySeller.sellerId()).isEqualTo(OWNER);
        }

        @Test
        @DisplayName("상대방이 확정하면 COMPLETED 와 완료 시각을 갖는다")
        void it_confirms_by_counterpart() {
            TradeComplete pending = TradeComplete.request(10L, new TradeSides(OTHER, OWNER, false));
            LocalDateTime now = LocalDateTime.of(2026, 3, 1, 12, 0);

            pending.confirm(true, now);

            assertThat(pending.status()).isEqualTo(TradeStatus.COMPLETED);
            assertThat(pending.completedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("본인이 만든 요청을 확정하면 TradeAlreadyCompleteRequestException 을 던지고 상태를 바꾸지 않는다")
        void it_rejects_self_confirm() {
            TradeComplete pending = TradeComplete.request(10L, new TradeSides(OTHER, OWNER, false));

            assertThatThrownBy(() -> pending.confirm(false, LocalDateTime.now()))
                    .isInstanceOf(TradeAlreadyCompleteRequestException.class);
            assertThat(pending.status()).isEqualTo(TradeStatus.PENDING);
        }

        @Test
        @DisplayName("작성자 즉시 완료는 요청 단계 없이 COMPLETED·완료 시각을 갖고 작성자 역할을 requestedBySeller 로 남긴다")
        void it_completes_by_author_without_request() {
            LocalDateTime now = LocalDateTime.of(2026, 3, 1, 12, 0);

            TradeComplete bySellerAuthor = TradeComplete.completeByAuthor(10L, new TradeSides(OTHER, OWNER, false), now);
            TradeComplete byBuyerAuthor = TradeComplete.completeByAuthor(10L, new TradeSides(OWNER, OTHER, true), now);

            assertThat(bySellerAuthor.status()).isEqualTo(TradeStatus.COMPLETED);
            assertThat(bySellerAuthor.completedAt()).isEqualTo(now);
            assertThat(bySellerAuthor.requestedBySeller()).isTrue();
            assertThat(byBuyerAuthor.requestedBySeller()).isFalse();
        }

        @Test
        @DisplayName("작성자는 자신이 만든 대기 요청도 확정할 수 있다")
        void it_confirms_own_pending_by_author() {
            TradeComplete own = TradeComplete.request(10L, new TradeSides(OTHER, OWNER, false));
            LocalDateTime now = LocalDateTime.of(2026, 3, 1, 12, 0);

            own.confirmByAuthor(now);

            assertThat(own.status()).isEqualTo(TradeStatus.COMPLETED);
            assertThat(own.completedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("요청자가 아닌 쪽의 요청 취소는 NotTradeCompleteRequesterException 이다")
        void it_rejects_withdraw_by_counterpart() {
            TradeComplete pending = TradeComplete.request(10L, new TradeSides(OTHER, OWNER, true));

            pending.validateWithdrawableBy(true);
            assertThatThrownBy(() -> pending.validateWithdrawableBy(false))
                    .isInstanceOf(NotTradeCompleteRequesterException.class);
        }
    }

    @Nested
    @DisplayName("TradeCancel 은")
    class Describe_trade_cancel {

        @Test
        @DisplayName("PENDING 이 아니면 승인할 수 없다")
        void it_approves_only_pending() {
            TradeCancel cancel = new TradeCancel(1L, 10L, OWNER, "사유", TradeCancelStatus.APPROVED);

            assertThatThrownBy(cancel::approve).isInstanceOf(CannotPendingTradeCancelException.class);
        }

        @Test
        @DisplayName("본인이 낸 요청에 다시 요청하면 AlreadyTradeCancelRequestException 이다")
        void it_rejects_repeated_request() {
            TradeCancel cancel = TradeCancel.request(10L, OWNER, "사유");

            assertThatThrownBy(() -> cancel.validateAgreementBy(OWNER)).isInstanceOf(AlreadyTradeCancelRequestException.class);
            cancel.validateAgreementBy(OTHER);
        }

        @Test
        @DisplayName("관리자 기각은 PENDING 만 REJECTED 로 바꾸고 나머지는 CannotPendingTradeCancelException 이다")
        void it_rejects_only_pending() {
            TradeCancel pending = TradeCancel.request(10L, OWNER, "사유");
            pending.reject();
            assertThat(pending.status()).isEqualTo(TradeCancelStatus.REJECTED);

            for (TradeCancelStatus done : List.of(TradeCancelStatus.APPROVED, TradeCancelStatus.REJECTED, TradeCancelStatus.WITHDRAWN)) {
                TradeCancel cancel = new TradeCancel(1L, 10L, OWNER, "사유", done);
                assertThatThrownBy(cancel::reject).isInstanceOf(CannotPendingTradeCancelException.class);
                assertThat(cancel.status()).isEqualTo(done);
            }
        }

        @Test
        @DisplayName("요청자만 철회 요청을 취소할 수 있다")
        void it_withdraws_by_requester_only() {
            TradeCancel cancel = TradeCancel.request(10L, OWNER, "사유");

            assertThatThrownBy(() -> cancel.withdraw(OTHER)).isInstanceOf(NotTradeCancelRequesterException.class);
            cancel.withdraw(OWNER);
            assertThat(cancel.status()).isEqualTo(TradeCancelStatus.WITHDRAWN);
        }
    }
}
