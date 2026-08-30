package team.startup.gwangsan.domain.trade.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 채팅방 조회 응답과 거래 상태 변경 이벤트가 같은 값을 쓰는지 고정하는 테스트.
 *
 * <p>거래 수명주기의 각 상태에서 방 전체로 보내도 되는 사실(completed / reserved /
 * requestedBySeller / requestedAt)과 보는 사람마다 달라지는 파생값(isCompletable)을
 * 구분해 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TradeStateReader 단위 테스트")
class TradeStateReaderTest {

    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 8, 30, 11, 20);

    @Mock
    private TradeCompleteRepository tradeCompleteRepository;

    @InjectMocks
    private TradeStateReader tradeStateReader;

    private final Product product = mock(Product.class);
    private final Member buyer = mock(Member.class);
    private final Member seller = mock(Member.class);

    private void givenProductStatus(ProductStatus status) {
        when(product.getStatus()).thenReturn(status);
    }

    private void givenNoTradeComplete() {
        when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(product, buyer, seller, TradeStatus.PENDING))
                .thenReturn(Optional.empty());
        when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(product, buyer, seller, TradeStatus.COMPLETED))
                .thenReturn(Optional.empty());
    }

    private void givenPendingRequest(boolean requestedBySeller) {
        TradeComplete pending = mock(TradeComplete.class);
        when(pending.isRequestedBySeller()).thenReturn(requestedBySeller);
        when(pending.getCreatedAt()).thenReturn(REQUESTED_AT);
        when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(product, buyer, seller, TradeStatus.PENDING))
                .thenReturn(Optional.of(pending));
    }

    private void givenCompletedRequestOnly() {
        TradeComplete completed = mock(TradeComplete.class);
        when(completed.getCreatedAt()).thenReturn(REQUESTED_AT);
        when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(product, buyer, seller, TradeStatus.PENDING))
                .thenReturn(Optional.empty());
        when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(product, buyer, seller, TradeStatus.COMPLETED))
                .thenReturn(Optional.of(completed));
    }

    private TradeStateSnapshot read() {
        return tradeStateReader.read(product, buyer, seller);
    }

    @Nested
    @DisplayName("read() 메서드는")
    class Describe_read {

        @Test
        @DisplayName("거래 요청이 없으면 양쪽 모두 거래를 요청할 수 있다")
        void it_allows_both_parties_when_no_request_exists() {
            givenProductStatus(ProductStatus.ONGOING);
            givenNoTradeComplete();

            TradeStateSnapshot snapshot = read();

            assertFalse(snapshot.completed());
            assertFalse(snapshot.reserved());
            assertNull(snapshot.requestedBySeller());
            assertNull(snapshot.requestedAt());
            assertTrue(snapshot.isCompletableFor(true));
            assertTrue(snapshot.isCompletableFor(false));
        }

        @Test
        @DisplayName("판매자가 완료를 요청하면 판매자 본인은 확정할 수 없고 구매자는 확정할 수 있다")
        void it_blocks_seller_when_seller_requested() {
            givenProductStatus(ProductStatus.ONGOING);
            givenPendingRequest(true);

            TradeStateSnapshot snapshot = read();

            assertFalse(snapshot.completed());
            assertEquals(Boolean.TRUE, snapshot.requestedBySeller());
            assertEquals(REQUESTED_AT, snapshot.requestedAt());
            assertFalse(snapshot.isCompletableFor(true));
            assertTrue(snapshot.isCompletableFor(false));
        }

        @Test
        @DisplayName("구매자가 완료를 요청하면 구매자 본인은 확정할 수 없고 판매자는 확정할 수 있다")
        void it_blocks_buyer_when_buyer_requested() {
            givenProductStatus(ProductStatus.ONGOING);
            givenPendingRequest(false);

            TradeStateSnapshot snapshot = read();

            assertFalse(snapshot.completed());
            assertEquals(Boolean.FALSE, snapshot.requestedBySeller());
            assertEquals(REQUESTED_AT, snapshot.requestedAt());
            assertTrue(snapshot.isCompletableFor(true));
            assertFalse(snapshot.isCompletableFor(false));
        }

        @Test
        @DisplayName("거래가 완료되면 양쪽 모두 확정할 수 없고 거래 요청 시각은 남는다")
        void it_keeps_requested_at_after_completion() {
            givenProductStatus(ProductStatus.COMPLETED);
            givenCompletedRequestOnly();

            TradeStateSnapshot snapshot = read();

            assertTrue(snapshot.completed());
            assertFalse(snapshot.reserved());
            assertNull(snapshot.requestedBySeller());
            // 값이 남아야 거래 카드가 유지되어 완료 표시와 리뷰 작성 진입점이 보인다.
            assertEquals(REQUESTED_AT, snapshot.requestedAt());
            assertFalse(snapshot.isCompletableFor(true));
            assertFalse(snapshot.isCompletableFor(false));
        }

        @Test
        @DisplayName("철회가 승인되면 거래 요청 흔적이 남지 않아 양쪽 모두 다시 요청할 수 있다")
        void it_clears_state_after_rollback() {
            // 철회 승인 후 TradeComplete 는 ROLLED_BACK 으로 남고 상품은 다시 거래 가능해진다.
            // ROLLED_BACK 행의 requestedBySeller 가 새어 나가면 판매자 화면의 isCompletable 이
            // 조회 응답과 어긋나므로, PENDING/COMPLETED 가 아닌 상태는 조회되지 않아야 한다.
            givenProductStatus(ProductStatus.ONGOING);
            givenNoTradeComplete();

            TradeStateSnapshot snapshot = read();

            assertFalse(snapshot.completed());
            assertNull(snapshot.requestedBySeller());
            assertNull(snapshot.requestedAt());
            assertTrue(snapshot.isCompletableFor(true));
            assertTrue(snapshot.isCompletableFor(false));
        }

        @Test
        @DisplayName("예약 상태에서 대기 중인 요청이 없으면 예약만 표시되고 양쪽 모두 요청할 수 있다")
        void it_marks_reserved_without_pending_request() {
            givenProductStatus(ProductStatus.RESERVATION);
            givenNoTradeComplete();

            TradeStateSnapshot snapshot = read();

            assertFalse(snapshot.completed());
            assertTrue(snapshot.reserved());
            assertNull(snapshot.requestedBySeller());
            assertTrue(snapshot.isCompletableFor(true));
            assertTrue(snapshot.isCompletableFor(false));
        }

        @Test
        @DisplayName("예약 상태에서 대기 중인 요청이 있으면 예약과 요청 상태를 함께 표시한다")
        void it_marks_reserved_with_pending_request() {
            givenProductStatus(ProductStatus.RESERVATION);
            givenPendingRequest(true);

            TradeStateSnapshot snapshot = read();

            assertFalse(snapshot.completed());
            assertTrue(snapshot.reserved());
            assertEquals(Boolean.TRUE, snapshot.requestedBySeller());
            assertEquals(REQUESTED_AT, snapshot.requestedAt());
            assertFalse(snapshot.isCompletableFor(true));
            assertTrue(snapshot.isCompletableFor(false));
        }
    }

    @Nested
    @DisplayName("isCompletableFor() 는")
    class Describe_isCompletableFor {

        @Test
        @DisplayName("거래가 완료되면 대기 중인 요청 정보와 무관하게 항상 false 다")
        void it_returns_false_when_completed_regardless_of_request() {
            TradeStateSnapshot completedWithRequester =
                    new TradeStateSnapshot(true, false, true, REQUESTED_AT);

            assertFalse(completedWithRequester.isCompletableFor(true));
            assertFalse(completedWithRequester.isCompletableFor(false));
        }

        @Test
        @DisplayName("클라이언트가 requestedBySeller 와 isSeller 로 같은 결과를 계산할 수 있다")
        void it_matches_client_side_formula() {
            // 클라이언트 공식:
            //   isCompletable = !isCompleted && (requestedBySeller == null || requestedBySeller !== isSeller)
            for (Boolean requestedBySeller : new Boolean[]{null, Boolean.TRUE, Boolean.FALSE}) {
                for (boolean completed : new boolean[]{true, false}) {
                    for (boolean isSeller : new boolean[]{true, false}) {
                        TradeStateSnapshot snapshot =
                                new TradeStateSnapshot(completed, false, requestedBySeller, REQUESTED_AT);

                        boolean clientSide = !completed
                                && (requestedBySeller == null || requestedBySeller != isSeller);

                        assertEquals(clientSide, snapshot.isCompletableFor(isSeller),
                                "completed=" + completed
                                        + ", requestedBySeller=" + requestedBySeller
                                        + ", isSeller=" + isSeller);
                    }
                }
            }
        }
    }
}
