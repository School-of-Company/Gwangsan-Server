package team.startup.gwangsan.application.trade.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.trade.data.*;
import team.startup.gwangsan.application.trade.port.in.TradeStateQuery;
import team.startup.gwangsan.application.trade.port.out.*;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.trade.exception.*;
import team.startup.gwangsan.domain.trade.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("거래 철회 유스케이스 단위 테스트")
class TradeCancelServicesTest {

    private static final long PRODUCT = 100L;
    private static final long TRADE = 10L;
    private static final long CANCEL = 20L;
    private static final long BUYER = 2L;
    private static final long SELLER = 1L;
    private static final long ROOM = 50L;
    private static final int GWANGSAN = 5_000;

    @Mock private TradeProductPort tradeProductPort;
    @Mock private TradeCompletePort tradeCompletePort;
    @Mock private TradeCancelPort tradeCancelPort;
    @Mock private TradeAlertPort tradeAlertPort;
    @Mock private TradeMemberPort tradeMemberPort;
    @Mock private TradeChatPort tradeChatPort;
    @Mock private TradeStateQuery tradeStateQuery;
    @Mock private TradeStatusPort tradeStatusPort;
    @Mock private TradeCancelApplier mockedApplier;
    @Mock private TradeImagePort tradeImagePort;

    private TradeComplete completed() {
        return new TradeComplete(TRADE, PRODUCT, BUYER, SELLER, TradeStatus.COMPLETED, true,
                LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    private TradeCancel pendingCancel(long requesterId) {
        return new TradeCancel(CANCEL, TRADE, requesterId, "사유", TradeCancelStatus.PENDING);
    }

    private TradeCancelApplier realApplier() {
        return new TradeCancelApplier(tradeProductPort, tradeMemberPort, tradeCancelPort, tradeCompletePort,
                tradeAlertPort, tradeChatPort, tradeStateQuery, tradeStatusPort);
    }

    @Nested
    @DisplayName("RequestTradeCancelService 는")
    class Describe_request {

        private RequestTradeCancelService service() {
            return new RequestTradeCancelService(tradeProductPort, tradeCompletePort, tradeCancelPort, mockedApplier, tradeAlertPort, tradeImagePort);
        }

        private TradeCancelResult execute(long actorId) {
            return service().execute(new RequestTradeCancelCommand(actorId, PRODUCT, "사유", List.of(5L)));
        }

        @Test
        @DisplayName("대기 요청이 없으면 철회 요청과 이미지를 저장하고 관리자 승인 대기 알림을 요청한다")
        void it_creates_request() {
            when(tradeCompletePort.findCompleted(PRODUCT)).thenReturn(Optional.of(completed()));
            when(tradeCancelPort.findPendingByTrade(TRADE)).thenReturn(Optional.empty());
            when(tradeImagePort.findExistingImageIds(List.of(5L))).thenReturn(List.of(5L));
            when(tradeCancelPort.create(any())).thenAnswer(inv -> {
                TradeCancel c = inv.getArgument(0);
                return new TradeCancel(CANCEL, c.tradeCompleteId(), c.requesterId(), c.reason(), c.status());
            });

            assertThat(execute(BUYER).cancelled()).isFalse();

            InOrder order = inOrder(tradeProductPort, tradeCompletePort, tradeCancelPort, tradeImagePort, tradeAlertPort);
            order.verify(tradeProductPort).lockIncludingDeleted(PRODUCT);
            order.verify(tradeCompletePort).findCompleted(PRODUCT);
            ArgumentCaptor<TradeCancel> created = ArgumentCaptor.forClass(TradeCancel.class);
            order.verify(tradeCancelPort).create(created.capture());
            order.verify(tradeImagePort).findExistingImageIds(List.of(5L));
            order.verify(tradeCancelPort).attachImages(CANCEL, List.of(5L));
            order.verify(tradeAlertPort).requestCancelReview(CANCEL, BUYER);
            assertThat(created.getValue().status()).isEqualTo(TradeCancelStatus.PENDING);
            assertThat(created.getValue().requesterId()).isEqualTo(BUYER);
            verifyNoInteractions(mockedApplier);
        }

        @Test
        @DisplayName("상대방이 이미 요청했으면 양측 동의로 즉시 철회하고 새 요청을 저장하지 않는다")
        void it_applies_on_agreement() {
            TradeComplete trade = completed();
            TradeCancel requested = pendingCancel(SELLER);
            when(tradeCompletePort.findCompleted(PRODUCT)).thenReturn(Optional.of(trade));
            when(tradeCancelPort.findPendingByTrade(TRADE)).thenReturn(Optional.of(requested));

            assertThat(execute(BUYER).cancelled()).isTrue();

            verify(mockedApplier).apply(requested, trade);
            verify(tradeCancelPort, never()).create(any());
            verifyNoInteractions(tradeImagePort);
            verifyNoInteractions(tradeAlertPort);
        }

        @Test
        @DisplayName("같은 요청자가 다시 요청하면 AlreadyTradeCancelRequestException 을 던진다")
        void it_rejects_repeat() {
            when(tradeCompletePort.findCompleted(PRODUCT)).thenReturn(Optional.of(completed()));
            when(tradeCancelPort.findPendingByTrade(TRADE)).thenReturn(Optional.of(pendingCancel(BUYER)));

            assertThatThrownBy(() -> execute(BUYER)).isInstanceOf(AlreadyTradeCancelRequestException.class);
            verifyNoInteractions(mockedApplier);
        }

        @Test
        @DisplayName("상품이 없으면 NotFoundProductException 을 던진다")
        void it_requires_product() {
            doThrow(new NotFoundProductException()).when(tradeProductPort).lockIncludingDeleted(PRODUCT);

            assertThatThrownBy(() -> execute(BUYER)).isInstanceOf(NotFoundProductException.class);
            verifyNoInteractions(tradeCompletePort);
        }

        @Test
        @DisplayName("완료된 거래가 없으면 NotFoundTradeCompleteException 을 던진다")
        void it_requires_completed_trade() {
            when(tradeCompletePort.findCompleted(PRODUCT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> execute(BUYER)).isInstanceOf(NotFoundTradeCompleteException.class);
        }

        @Test
        @DisplayName("거래 참여자가 아니면 TradeParticipantOnlyException 을 던진다")
        void it_requires_participant() {
            when(tradeCompletePort.findCompleted(PRODUCT)).thenReturn(Optional.of(completed()));

            assertThatThrownBy(() -> execute(99L)).isInstanceOf(TradeParticipantOnlyException.class);
            verifyNoInteractions(tradeCancelPort);
        }
    }

    @Nested
    @DisplayName("WithdrawTradeCancelService 는")
    class Describe_withdraw {

        private void execute(long actorId) {
            new WithdrawTradeCancelService(tradeProductPort, tradeCompletePort, tradeCancelPort)
                    .execute(new WithdrawTradeCancelCommand(actorId, PRODUCT));
        }

        @Test
        @DisplayName("요청자 본인이면 WITHDRAWN 으로 저장하고 이미지를 지운다")
        void it_withdraws() {
            when(tradeCompletePort.findCompleted(PRODUCT)).thenReturn(Optional.of(completed()));
            when(tradeCancelPort.findPendingByTrade(TRADE)).thenReturn(Optional.of(pendingCancel(BUYER)));

            execute(BUYER);

            ArgumentCaptor<TradeCancel> updated = ArgumentCaptor.forClass(TradeCancel.class);
            InOrder order = inOrder(tradeProductPort, tradeCancelPort);
            order.verify(tradeProductPort).lockIncludingDeleted(PRODUCT);
            order.verify(tradeCancelPort).update(updated.capture());
            order.verify(tradeCancelPort).deleteImages(CANCEL);
            assertThat(updated.getValue().status()).isEqualTo(TradeCancelStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("상품이 없으면 NotFoundProductException 을 던지고 후속 작업을 하지 않는다")
        void it_requires_product() {
            doThrow(new NotFoundProductException()).when(tradeProductPort).lockIncludingDeleted(PRODUCT);

            assertThatThrownBy(() -> execute(BUYER)).isInstanceOf(NotFoundProductException.class);
            verifyNoInteractions(tradeCompletePort, tradeCancelPort);
        }

        @Test
        @DisplayName("완료된 거래가 없으면 NotFoundTradeCompleteException 을 던진다")
        void it_requires_trade() {
            when(tradeCompletePort.findCompleted(PRODUCT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> execute(BUYER)).isInstanceOf(NotFoundTradeCompleteException.class);
        }

        @Test
        @DisplayName("대기 철회 요청이 없으면 NotFoundTradeCancelException 을 던진다")
        void it_requires_cancel() {
            when(tradeCompletePort.findCompleted(PRODUCT)).thenReturn(Optional.of(completed()));
            when(tradeCancelPort.findPendingByTrade(TRADE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> execute(BUYER)).isInstanceOf(NotFoundTradeCancelException.class);
        }

        @Test
        @DisplayName("요청자가 아니면 NotTradeCancelRequesterException 을 던지고 저장하지 않는다")
        void it_requires_requester() {
            when(tradeCompletePort.findCompleted(PRODUCT)).thenReturn(Optional.of(completed()));
            when(tradeCancelPort.findPendingByTrade(TRADE)).thenReturn(Optional.of(pendingCancel(BUYER)));

            assertThatThrownBy(() -> execute(SELLER)).isInstanceOf(NotTradeCancelRequesterException.class);
            verify(tradeCancelPort, never()).update(any());
            verify(tradeCancelPort, never()).deleteImages(anyLong());
        }
    }

    @Nested
    @DisplayName("ApproveTradeCancelService 는")
    class Describe_approve {

        private void execute(ApproveTradeCancelCommand.ParticipantAuthorizer authorizer) {
            new ApproveTradeCancelService(tradeProductPort, tradeCancelPort, tradeCompletePort, mockedApplier)
                    .execute(new ApproveTradeCancelCommand(CANCEL, authorizer));
        }

        @Test
        @DisplayName("상품 ID 스칼라 조회 → 상품 잠금 → 철회 요청 최초 로드 → 관리자 인가 → 반영 순서를 지킨다")
        void it_keeps_lock_order() {
            TradeCancel cancel = pendingCancel(BUYER);
            TradeComplete trade = completed();
            ApproveTradeCancelCommand.ParticipantAuthorizer authorizer = mock(ApproveTradeCancelCommand.ParticipantAuthorizer.class);
            when(tradeCancelPort.findProductId(CANCEL)).thenReturn(Optional.of(PRODUCT));
            when(tradeCancelPort.findById(CANCEL)).thenReturn(Optional.of(cancel));
            when(tradeCompletePort.findById(TRADE)).thenReturn(Optional.of(trade));

            execute(authorizer);

            InOrder order = inOrder(tradeCancelPort, tradeProductPort, authorizer, mockedApplier);
            order.verify(tradeCancelPort).findProductId(CANCEL);
            order.verify(tradeProductPort).lockIncludingDeleted(PRODUCT);
            order.verify(tradeCancelPort).findById(CANCEL);
            order.verify(authorizer).authorize(BUYER, SELLER);
            order.verify(mockedApplier).apply(cancel, trade);
        }

        @Test
        @DisplayName("관리자 인가가 실패하면 철회를 반영하지 않는다")
        void it_stops_on_authorization_failure() {
            when(tradeCancelPort.findProductId(CANCEL)).thenReturn(Optional.of(PRODUCT));
            when(tradeCancelPort.findById(CANCEL)).thenReturn(Optional.of(pendingCancel(BUYER)));
            when(tradeCompletePort.findById(TRADE)).thenReturn(Optional.of(completed()));

            assertThatThrownBy(() -> execute((b, s) -> { throw new IllegalStateException("다른 지점"); }))
                    .isInstanceOf(IllegalStateException.class);
            verifyNoInteractions(mockedApplier);
        }

        @Test
        @DisplayName("철회 요청이 없으면 잠금 전에 NotFoundTradeCancelException 을 던진다")
        void it_requires_cancel() {
            when(tradeCancelPort.findProductId(CANCEL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> execute((b, s) -> {})).isInstanceOf(NotFoundTradeCancelException.class);
            verifyNoInteractions(tradeProductPort);
        }

        @Test
        @DisplayName("상품이 없으면 NotFoundProductException 을 던진다")
        void it_requires_product() {
            when(tradeCancelPort.findProductId(CANCEL)).thenReturn(Optional.of(PRODUCT));
            doThrow(new NotFoundProductException()).when(tradeProductPort).lockIncludingDeleted(PRODUCT);

            assertThatThrownBy(() -> execute((b, s) -> {})).isInstanceOf(NotFoundProductException.class);
            verify(tradeCancelPort, never()).findById(anyLong());
        }
    }

    @Nested
    @DisplayName("TradeCancelApplier 는")
    class Describe_applier {

        @Test
        @DisplayName("환불·상태 전이·상품 재개·관리자 알림 제거·철회 알림 저장 후 대기 요청 없는 상태를 전달한다")
        void it_applies_cancel() {
            TradeCancel cancel = pendingCancel(BUYER);
            TradeComplete trade = completed();
            when(tradeProductPort.find(PRODUCT)).thenReturn(new TradeProduct(PRODUCT, SELLER, true, ProductPhase.COMPLETED, GWANGSAN));
            when(tradeChatPort.findRoomId(PRODUCT, BUYER, SELLER)).thenReturn(Optional.of(ROOM));
            when(tradeStateQuery.query(new TradeStateKey(PRODUCT, BUYER, SELLER)))
                    .thenReturn(new TradeStateSnapshot(false, false, null, null));

            realApplier().apply(cancel, trade);

            assertThat(cancel.status()).isEqualTo(TradeCancelStatus.APPROVED);
            assertThat(trade.status()).isEqualTo(TradeStatus.ROLLED_BACK);
            InOrder order = inOrder(tradeMemberPort, tradeCancelPort, tradeCompletePort, tradeProductPort, tradeAlertPort, tradeStatusPort);
            order.verify(tradeMemberPort).transferForCancel(SELLER, BUYER, GWANGSAN, CANCEL);
            order.verify(tradeCancelPort).update(cancel);
            order.verify(tradeCompletePort).update(trade);
            order.verify(tradeProductPort).reopen(PRODUCT);
            order.verify(tradeAlertPort).removeCancelReview(CANCEL);
            order.verify(tradeAlertPort).recordCancelApproved(CANCEL, BUYER, BUYER, SELLER);
            order.verify(tradeStatusPort).publish(new TradeStatusChange(ROOM, PRODUCT, false, false, null, null));
        }

        @Test
        @DisplayName("채팅방이 없으면 상태를 전달하지 않는다")
        void it_skips_publish_without_room() {
            when(tradeProductPort.find(PRODUCT)).thenReturn(new TradeProduct(PRODUCT, SELLER, true, ProductPhase.COMPLETED, GWANGSAN));
            when(tradeChatPort.findRoomId(PRODUCT, BUYER, SELLER)).thenReturn(Optional.empty());

            realApplier().apply(pendingCancel(BUYER), completed());

            verifyNoInteractions(tradeStatusPort, tradeStateQuery);
        }

        @Test
        @DisplayName("PENDING 이 아니면 CannotPendingTradeCancelException 을 던지고 아무것도 바꾸지 않는다")
        void it_rejects_non_pending() {
            TradeCancel approved = new TradeCancel(CANCEL, TRADE, BUYER, "사유", TradeCancelStatus.APPROVED);

            assertThatThrownBy(() -> realApplier().apply(approved, completed()))
                    .isInstanceOf(CannotPendingTradeCancelException.class);
            verifyNoInteractions(tradeMemberPort, tradeCancelPort, tradeCompletePort, tradeProductPort, tradeAlertPort);
        }

        @Test
        @DisplayName("회원 상세가 없으면 NotFoundMemberDetailException 을 던지고 상태를 저장하지 않는다")
        void it_propagates_missing_member_detail() {
            when(tradeProductPort.find(PRODUCT)).thenReturn(new TradeProduct(PRODUCT, SELLER, true, ProductPhase.COMPLETED, GWANGSAN));
            doThrow(new NotFoundMemberDetailException()).when(tradeMemberPort).transferForCancel(SELLER, BUYER, GWANGSAN, CANCEL);

            assertThatThrownBy(() -> realApplier().apply(pendingCancel(BUYER), completed()))
                    .isInstanceOf(NotFoundMemberDetailException.class);
            verifyNoInteractions(tradeCancelPort, tradeCompletePort, tradeAlertPort);
        }
    }
}
