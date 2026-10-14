package team.startup.gwangsan.application.trade.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.trade.data.TradeCompletionCommand;
import team.startup.gwangsan.application.trade.data.TradeStateKey;
import team.startup.gwangsan.application.trade.data.TradeStateSnapshot;
import team.startup.gwangsan.application.trade.data.TradeStatusChange;
import team.startup.gwangsan.application.trade.port.in.TradeStateQuery;
import team.startup.gwangsan.application.trade.port.out.*;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.trade.exception.*;
import team.startup.gwangsan.domain.trade.model.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawTradeCompletionService 단위 테스트")
class WithdrawTradeCompletionServiceTest {

    private static final long PRODUCT = 100L;
    private static final long SELLER = 1L;
    private static final long BUYER = 2L;
    private static final long ROOM = 50L;

    @InjectMocks private WithdrawTradeCompletionService service;

    @Mock private TradeProductPort tradeProductPort;
    @Mock private TradeMemberPort tradeMemberPort;
    @Mock private TradeCompletePort tradeCompletePort;
    @Mock private TradeChatPort tradeChatPort;
    @Mock private TradeStateQuery tradeStateQuery;
    @Mock private TradeStatusPort tradeStatusPort;

    private void givenProduct(long ownerId, boolean ownerIsSeller, ProductPhase phase) {
        when(tradeProductPort.lockActive(PRODUCT)).thenReturn(new TradeProduct(PRODUCT, ownerId, ownerIsSeller, phase, 100));
    }

    private void givenPending(boolean bySeller) {
        when(tradeCompletePort.findPending(PRODUCT, BUYER, SELLER)).thenReturn(Optional.of(
                new TradeComplete(7L, PRODUCT, BUYER, SELLER, TradeStatus.PENDING, bySeller, LocalDateTime.now(), null)));
    }

    private void execute(long actorId, long otherId) {
        service.execute(new TradeCompletionCommand(actorId, PRODUCT, otherId));
    }

    @Test
    @DisplayName("본인과 거래를 시도하면 CannotSelectSelfException 을 던진다")
    void it_rejects_self() {
        assertThatThrownBy(() -> execute(SELLER, SELLER)).isInstanceOf(CannotSelectSelfException.class);
        verifyNoInteractions(tradeProductPort);
    }

    @Test
    @DisplayName("상품이 없으면 NotFoundProductException 을 던진다")
    void it_propagates_missing_product() {
        when(tradeProductPort.lockActive(PRODUCT)).thenThrow(new NotFoundProductException());

        assertThatThrownBy(() -> execute(SELLER, BUYER)).isInstanceOf(NotFoundProductException.class);
    }

    @Test
    @DisplayName("이미 거래 완료된 상품이면 TradeAlreadyCompleteException 을 던진다")
    void it_rejects_completed() {
        givenProduct(SELLER, true, ProductPhase.COMPLETED);

        assertThatThrownBy(() -> execute(SELLER, BUYER)).isInstanceOf(TradeAlreadyCompleteException.class);
    }

    @Test
    @DisplayName("대기 중인 요청이 없으면 NotFoundTradeCompleteException 을 던진다")
    void it_requires_pending() {
        givenProduct(SELLER, true, ProductPhase.ONGOING);
        when(tradeCompletePort.findPending(PRODUCT, BUYER, SELLER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> execute(SELLER, BUYER)).isInstanceOf(NotFoundTradeCompleteException.class);
    }

    @Test
    @DisplayName("요청자가 아닌 상대방이 취소하면 NotTradeCompleteRequesterException 을 던진다")
    void it_rejects_counterpart() {
        givenProduct(SELLER, true, ProductPhase.ONGOING);
        givenPending(true);

        assertThatThrownBy(() -> execute(BUYER, SELLER)).isInstanceOf(NotTradeCompleteRequesterException.class);
        verify(tradeCompletePort, never()).delete(anyLong());
    }

    @Test
    @DisplayName("채팅방이 없으면 NotFoundChatRoomException 을 던지고 삭제하지 않는다")
    void it_requires_room() {
        givenProduct(SELLER, true, ProductPhase.ONGOING);
        givenPending(true);
        when(tradeChatPort.requireRoomId(PRODUCT, BUYER, SELLER)).thenThrow(new NotFoundChatRoomException());

        assertThatThrownBy(() -> execute(SELLER, BUYER)).isInstanceOf(NotFoundChatRoomException.class);
        verify(tradeCompletePort, never()).delete(anyLong());
    }

    @Test
    @DisplayName("요청자 본인이 취소하면 PENDING 을 지운 뒤 최신 상태를 다시 읽어 전달한다")
    void it_withdraws_and_publishes_latest_state() {
        givenProduct(SELLER, true, ProductPhase.RESERVED);
        givenPending(true);
        when(tradeChatPort.requireRoomId(PRODUCT, BUYER, SELLER)).thenReturn(ROOM);
        when(tradeStateQuery.query(new TradeStateKey(PRODUCT, BUYER, SELLER)))
                .thenReturn(new TradeStateSnapshot(false, true, null, null));

        execute(SELLER, BUYER);

        InOrder order = inOrder(tradeCompletePort, tradeStateQuery, tradeStatusPort);
        order.verify(tradeCompletePort).delete(7L);
        order.verify(tradeStateQuery).query(new TradeStateKey(PRODUCT, BUYER, SELLER));
        order.verify(tradeStatusPort).publish(new TradeStatusChange(ROOM, PRODUCT, false, true, null, null));
    }

    @Test
    @DisplayName("RECEIVER 글 작성자인 구매자가 자신의 요청을 취소한다")
    void it_withdraws_receiver_owner_request() {
        givenProduct(BUYER, false, ProductPhase.ONGOING);
        givenPending(false);
        when(tradeChatPort.requireRoomId(PRODUCT, BUYER, SELLER)).thenReturn(ROOM);
        when(tradeStateQuery.query(new TradeStateKey(PRODUCT, BUYER, SELLER)))
                .thenReturn(new TradeStateSnapshot(false, false, null, null));

        execute(BUYER, SELLER);

        verify(tradeCompletePort).delete(7L);
    }
}
