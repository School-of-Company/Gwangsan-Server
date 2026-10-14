package team.startup.gwangsan.application.trade.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.trade.data.TradeCompletionCommand;
import team.startup.gwangsan.application.trade.data.TradeStatusChange;
import team.startup.gwangsan.application.trade.port.out.*;
import team.startup.gwangsan.domain.block.exception.BlockedMemberException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ReservationParticipantOnlyException;
import team.startup.gwangsan.domain.trade.exception.CannotSelectSelfException;
import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteException;
import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteRequestException;
import team.startup.gwangsan.domain.trade.exception.TradeCompleteWithoutChattingException;
import team.startup.gwangsan.domain.trade.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestTradeCompletionService 단위 테스트")
class RequestTradeCompletionServiceTest {

    private static final long PRODUCT = 100L;
    private static final long SELLER = 1L;
    private static final long BUYER = 2L;
    private static final long ROOM = 50L;
    private static final int GWANGSAN = 3_000;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 1, 10, 0);

    @InjectMocks private RequestTradeCompletionService service;

    @Mock private TradeBlockPort tradeBlockPort;
    @Mock private TradeProductPort tradeProductPort;
    @Mock private TradeMemberPort tradeMemberPort;
    @Mock private TradeReservationPort tradeReservationPort;
    @Mock private TradeChatPort tradeChatPort;
    @Mock private TradeCompletePort tradeCompletePort;
    @Mock private TradeAlertPort tradeAlertPort;
    @Mock private TradePushPort tradePushPort;
    @Mock private TradeStatusPort tradeStatusPort;

    /** GIVER 글: 작성자(SELLER)가 판매자. */
    private void givenProduct(ProductPhase phase) {
        when(tradeProductPort.lockActive(PRODUCT)).thenReturn(new TradeProduct(PRODUCT, SELLER, true, phase, GWANGSAN));
    }

    private void givenChat(long actorId) {
        when(tradeChatPort.requireRoomId(PRODUCT, BUYER, SELLER)).thenReturn(ROOM);
        when(tradeChatPort.hasMessageFrom(ROOM, actorId)).thenReturn(true);
    }

    private void execute(long actorId, long otherId) {
        service.execute(new TradeCompletionCommand(actorId, PRODUCT, otherId));
    }

    @Nested
    @DisplayName("검증 단계에서")
    class Describe_validation {

        @Test
        @DisplayName("차단 관계를 다른 검증보다 먼저 확인한다")
        void it_checks_block_first() {
            doThrow(new BlockedMemberException()).when(tradeBlockPort).validateNotBlocked(SELLER, SELLER);

            assertThatThrownBy(() -> execute(SELLER, SELLER)).isInstanceOf(BlockedMemberException.class);
            verifyNoInteractions(tradeProductPort);
        }

        @Test
        @DisplayName("본인과 거래를 시도하면 CannotSelectSelfException 을 던진다")
        void it_rejects_self_trade() {
            assertThatThrownBy(() -> execute(SELLER, SELLER)).isInstanceOf(CannotSelectSelfException.class);
            verifyNoInteractions(tradeProductPort);
        }

        @Test
        @DisplayName("상품이 없으면 상품 소유자의 NotFoundProductException 을 그대로 던진다")
        void it_propagates_missing_product() {
            when(tradeProductPort.lockActive(PRODUCT)).thenThrow(new NotFoundProductException());

            assertThatThrownBy(() -> execute(SELLER, BUYER)).isInstanceOf(NotFoundProductException.class);
        }

        @Test
        @DisplayName("이미 거래 완료된 상품이면 TradeAlreadyCompleteException 을 던진다")
        void it_rejects_completed_product() {
            givenProduct(ProductPhase.COMPLETED);

            assertThatThrownBy(() -> execute(SELLER, BUYER)).isInstanceOf(TradeAlreadyCompleteException.class);
            verifyNoInteractions(tradeCompletePort);
        }

        @Test
        @DisplayName("채팅방이 없으면 NotFoundChatRoomException 을 던진다")
        void it_requires_chat_room() {
            givenProduct(ProductPhase.ONGOING);
            when(tradeChatPort.requireRoomId(PRODUCT, BUYER, SELLER)).thenThrow(new NotFoundChatRoomException());

            assertThatThrownBy(() -> execute(SELLER, BUYER)).isInstanceOf(NotFoundChatRoomException.class);
        }

        @Test
        @DisplayName("본인 채팅 내역이 없으면 TradeCompleteWithoutChattingException 을 던진다")
        void it_requires_chat_history() {
            givenProduct(ProductPhase.ONGOING);
            when(tradeChatPort.requireRoomId(PRODUCT, BUYER, SELLER)).thenReturn(ROOM);

            assertThatThrownBy(() -> execute(SELLER, BUYER)).isInstanceOf(TradeCompleteWithoutChattingException.class);
            verifyNoInteractions(tradeCompletePort);
        }

        @Test
        @DisplayName("예약 상품에서 예약 당사자가 아니면 ReservationParticipantOnlyException 을 던진다")
        void it_requires_reservation_participant() {
            givenProduct(ProductPhase.RESERVED);
            when(tradeReservationPort.requireParticipantReservation(PRODUCT, BUYER, SELLER))
                    .thenThrow(new ReservationParticipantOnlyException());

            assertThatThrownBy(() -> execute(BUYER, SELLER)).isInstanceOf(ReservationParticipantOnlyException.class);
            verifyNoInteractions(tradeChatPort);
        }
    }

    private void givenCreateReturnsSaved() {
        when(tradeCompletePort.create(any())).thenAnswer(inv -> {
            TradeComplete t = inv.getArgument(0);
            return new TradeComplete(9L, t.productId(), t.buyerId(), t.sellerId(), t.status(), t.requestedBySeller(),
                    CREATED_AT, t.completedAt());
        });
    }

    private TradeComplete pending(boolean bySeller) {
        return new TradeComplete(7L, PRODUCT, BUYER, SELLER, TradeStatus.PENDING, bySeller, CREATED_AT, null);
    }

    /** 완료 공통 처리: 상품 완료 → 거래 저장 → 잔액 1회 → (예약) → 커밋 후 푸시 요청 → 남은 대기 삭제 → 상태 전달. */
    private void verifyCompleted(TradeComplete trade, Long reservationId) {
        assertThat(trade.status()).isEqualTo(TradeStatus.COMPLETED);
        assertThat(trade.completedAt()).isNotNull();
        InOrder order = inOrder(tradeProductPort, tradeCompletePort, tradeMemberPort, tradeReservationPort, tradePushPort, tradeStatusPort);
        order.verify(tradeProductPort).complete(PRODUCT);
        order.verify(tradeCompletePort).update(trade);
        order.verify(tradeMemberPort).transfer(BUYER, SELLER, GWANGSAN);
        if (reservationId != null) {
            order.verify(tradeReservationPort).completeReservation(reservationId);
        }
        order.verify(tradePushPort).notifyCompleted(List.of(BUYER, SELLER), PRODUCT);
        order.verify(tradeCompletePort).deleteOtherPending(PRODUCT);
        order.verify(tradeStatusPort).publish(new TradeStatusChange(ROOM, PRODUCT, true, false, null, CREATED_AT));
        verify(tradeMemberPort, times(1)).transfer(anyLong(), anyLong(), anyInt());
        // 완료(확정) 알림함 기록은 신규로 만들지 않는다(확정 정책). 완료 요청 알림도 요청 단계가 없으므로 없다.
        verifyNoInteractions(tradeAlertPort);
    }

    @Nested
    @DisplayName("게시글 작성자가 호출하면")
    class Describe_author {

        @Test
        @DisplayName("대기 요청이 없으면 요청 단계 없이 COMPLETED 거래를 만들고 즉시 완료한다")
        void it_completes_immediately_without_pending() {
            givenProduct(ProductPhase.ONGOING);
            givenChat(SELLER);
            when(tradeCompletePort.findPending(PRODUCT, BUYER, SELLER)).thenReturn(Optional.empty());
            givenCreateReturnsSaved();

            execute(SELLER, BUYER);

            ArgumentCaptor<TradeComplete> created = ArgumentCaptor.forClass(TradeComplete.class);
            verify(tradeCompletePort).create(created.capture());
            assertThat(created.getValue().status()).isEqualTo(TradeStatus.COMPLETED);
            assertThat(created.getValue().requestedBySeller()).isTrue();
            assertThat(created.getValue().completedAt()).isNotNull();
            ArgumentCaptor<TradeComplete> updated = ArgumentCaptor.forClass(TradeComplete.class);
            verify(tradeCompletePort).update(updated.capture());
            verifyCompleted(updated.getValue(), null);
        }

        @ParameterizedTest(name = "requestedBySeller={0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("대기 요청이 있으면 요청자가 누구든 그 요청을 완료한다")
        void it_completes_any_pending(boolean requestedBySeller) {
            givenProduct(ProductPhase.ONGOING);
            givenChat(SELLER);
            TradeComplete pending = pending(requestedBySeller);
            when(tradeCompletePort.findPending(PRODUCT, BUYER, SELLER)).thenReturn(Optional.of(pending));

            execute(SELLER, BUYER);

            verify(tradeCompletePort, never()).create(any());
            verifyCompleted(pending, null);
        }

        @ParameterizedTest(name = "withOwnPending={0}")
        @ValueSource(booleans = {false, true})
        @DisplayName("RECEIVER 글 작성자는 구매자로 즉시 완료하고 광산을 판매자에게 보낸다")
        void it_completes_as_receiver_author(boolean withOwnPending) {
            when(tradeProductPort.lockActive(PRODUCT)).thenReturn(new TradeProduct(PRODUCT, BUYER, false, ProductPhase.ONGOING, GWANGSAN));
            givenChat(BUYER);
            TradeComplete own = pending(false);
            when(tradeCompletePort.findPending(PRODUCT, BUYER, SELLER)).thenReturn(withOwnPending ? Optional.of(own) : Optional.empty());
            if (!withOwnPending) {
                givenCreateReturnsSaved();
            }

            execute(BUYER, SELLER);

            ArgumentCaptor<TradeComplete> updated = ArgumentCaptor.forClass(TradeComplete.class);
            verify(tradeCompletePort).update(updated.capture());
            assertThat(updated.getValue().requestedBySeller()).isFalse();
            verify(tradeCompletePort, withOwnPending ? never() : times(1)).create(any());
            verifyCompleted(updated.getValue(), null);
        }

        @Test
        @DisplayName("예약자가 상대인 예약 상품도 즉시 완료하고 예약을 완료한다")
        void it_completes_reserved_product() {
            givenProduct(ProductPhase.RESERVED);
            givenChat(SELLER);
            when(tradeReservationPort.requireParticipantReservation(PRODUCT, BUYER, SELLER)).thenReturn(30L);
            when(tradeCompletePort.findPending(PRODUCT, BUYER, SELLER)).thenReturn(Optional.empty());
            givenCreateReturnsSaved();

            execute(SELLER, BUYER);

            ArgumentCaptor<TradeComplete> updated = ArgumentCaptor.forClass(TradeComplete.class);
            verify(tradeCompletePort).update(updated.capture());
            verifyCompleted(updated.getValue(), 30L);
        }
    }

    @Nested
    @DisplayName("비작성자가 호출하면")
    class Describe_non_author {

        @Test
        @DisplayName("대기 요청이 없으면 PENDING 을 만들고 작성자에게 보낼 완료 요청 알림을 같은 트랜잭션에 저장한다")
        void it_creates_pending_by_buyer() {
            givenProduct(ProductPhase.ONGOING);
            givenChat(BUYER);
            when(tradeCompletePort.findPending(PRODUCT, BUYER, SELLER)).thenReturn(Optional.empty());
            givenCreateReturnsSaved();

            execute(BUYER, SELLER);

            ArgumentCaptor<TradeComplete> created = ArgumentCaptor.forClass(TradeComplete.class);
            verify(tradeCompletePort).create(created.capture());
            assertThat(created.getValue().status()).isEqualTo(TradeStatus.PENDING);
            assertThat(created.getValue().requestedBySeller()).isFalse();
            assertThat(created.getValue().completedAt()).isNull();
            verify(tradeAlertPort).recordCompletionRequested(PRODUCT, BUYER, SELLER, SELLER);
            verify(tradeStatusPort).publish(new TradeStatusChange(ROOM, PRODUCT, false, false, false, CREATED_AT));
            verify(tradeProductPort, never()).complete(anyLong());
            verify(tradeMemberPort, never()).transfer(anyLong(), anyLong(), anyInt());
            verifyNoInteractions(tradePushPort);
        }

        @Test
        @DisplayName("RECEIVER 글의 비작성자 판매자는 requestedBySeller=true 로 요청한다")
        void it_creates_pending_by_non_author_seller() {
            when(tradeProductPort.lockActive(PRODUCT)).thenReturn(new TradeProduct(PRODUCT, BUYER, false, ProductPhase.ONGOING, GWANGSAN));
            givenChat(SELLER);
            when(tradeCompletePort.findPending(PRODUCT, BUYER, SELLER)).thenReturn(Optional.empty());
            givenCreateReturnsSaved();

            execute(SELLER, BUYER);

            verify(tradeAlertPort).recordCompletionRequested(PRODUCT, BUYER, SELLER, BUYER);
            verify(tradeStatusPort).publish(new TradeStatusChange(ROOM, PRODUCT, false, false, true, CREATED_AT));
        }

        @Test
        @DisplayName("예약 상품 요청이면 예약 사실을 상태 전달에 담고 예약은 그대로 둔다")
        void it_publishes_reserved() {
            givenProduct(ProductPhase.RESERVED);
            givenChat(BUYER);
            when(tradeReservationPort.requireParticipantReservation(PRODUCT, BUYER, SELLER)).thenReturn(30L);
            when(tradeCompletePort.findPending(PRODUCT, BUYER, SELLER)).thenReturn(Optional.empty());
            givenCreateReturnsSaved();

            execute(BUYER, SELLER);

            verify(tradeStatusPort).publish(new TradeStatusChange(ROOM, PRODUCT, false, true, false, CREATED_AT));
            verify(tradeReservationPort, never()).completeReservation(anyLong());
        }

        @Test
        @DisplayName("상대방(작성자)의 대기 요청을 확정하면 완료한다")
        void it_confirms_counterpart_request() {
            givenProduct(ProductPhase.ONGOING);
            givenChat(BUYER);
            TradeComplete pending = pending(true);
            when(tradeCompletePort.findPending(PRODUCT, BUYER, SELLER)).thenReturn(Optional.of(pending));

            execute(BUYER, SELLER);

            verifyCompleted(pending, null);
        }

        @Test
        @DisplayName("자신이 보낸 대기 요청을 다시 호출하면 TradeAlreadyCompleteRequestException 을 던지고 아무것도 바꾸지 않는다")
        void it_rejects_self_confirm() {
            givenProduct(ProductPhase.ONGOING);
            givenChat(BUYER);
            when(tradeCompletePort.findPending(PRODUCT, BUYER, SELLER)).thenReturn(Optional.of(pending(false)));

            assertThatThrownBy(() -> execute(BUYER, SELLER)).isInstanceOf(TradeAlreadyCompleteRequestException.class);
            verify(tradeProductPort, never()).complete(anyLong());
            verify(tradeCompletePort, never()).update(any());
            verify(tradeMemberPort, never()).transfer(anyLong(), anyLong(), anyInt());
            verifyNoInteractions(tradePushPort, tradeStatusPort, tradeAlertPort);
        }
    }
}
