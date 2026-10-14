package team.startup.gwangsan.application.post.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.post.data.ChatRoomView;
import team.startup.gwangsan.application.post.data.ReservationCancellation;
import team.startup.gwangsan.application.post.data.ReserveProductCommand;
import team.startup.gwangsan.application.post.port.out.ProductChatPort;
import team.startup.gwangsan.application.post.port.out.ProductMemberPort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.application.post.port.out.ReservationNotificationPort;
import team.startup.gwangsan.application.post.port.out.ReservationPort;
import team.startup.gwangsan.application.post.port.out.TradeStatusPort;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.ReservationStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ProductAlreadyReservationException;
import team.startup.gwangsan.domain.post.exception.ReservationChatRoomNotFoundException;
import team.startup.gwangsan.domain.post.exception.ReservationParticipantOnlyException;
import team.startup.gwangsan.domain.post.model.Product;
import team.startup.gwangsan.domain.post.model.ProductReservation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("예약 생성·취소·reminder 유스케이스")
class ReservationServiceTest {

    private static final Long SELLER = 10L;
    private static final Long BUYER = 20L;

    @Mock ProductPort productPort;
    @Mock ReservationPort reservationPort;
    @Mock ProductChatPort chatPort;
    @Mock ProductMemberPort memberPort;
    @Mock TradeStatusPort tradeStatusPort;
    @Mock ReservationNotificationPort notificationPort;

    private static Product product(ProductStatus status) {
        return new Product(7L, SELLER, Type.OBJECT, Mode.GIVER, "제목", "설명", 5000, status);
    }

    private static LocalDateTime seoulNow() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    private static ProductReservation pending(LocalDateTime scheduledAt, LocalDateTime reminder30SentAt) {
        return new ProductReservation(4L, 7L, BUYER, ReservationStatus.PENDING, scheduledAt, "광산구청", "주소",
                35.1, 126.7, reminder30SentAt, null);
    }

    @Nested
    @DisplayName("예약 생성은")
    class Describe_reserve {

        private ReserveProductService service() {
            return new ReserveProductService(productPort, reservationPort, chatPort, tradeStatusPort);
        }

        private ReserveProductCommand command(Long actorId, LocalDateTime scheduledAt) {
            return new ReserveProductCommand(actorId, 7L, 9L, scheduledAt, "광산구청", "광주 광산구", 35.139, 126.793);
        }

        @Test
        @DisplayName("방의 상대를 예약자로 저장하고 상품을 RESERVATION 으로 저장한 뒤 거래 상태를 전달한다")
        void it_reserves() {
            Product product = product(ProductStatus.ONGOING);
            when(productPort.lockActive(7L)).thenReturn(Optional.of(product));
            when(chatPort.findRoom(9L)).thenReturn(Optional.of(new ChatRoomView(9L, 7L, BUYER, SELLER)));
            LocalDateTime scheduledAt = seoulNow().plusDays(1);

            service().execute(command(SELLER, scheduledAt));

            ArgumentCaptor<ProductReservation> saved = ArgumentCaptor.forClass(ProductReservation.class);
            InOrder order = inOrder(reservationPort, productPort, tradeStatusPort);
            order.verify(reservationPort).save(saved.capture());
            order.verify(productPort).save(product);
            order.verify(tradeStatusPort).publishChanged(9L, 7L, BUYER, SELLER);
            assertThat(saved.getValue().getReserverId()).isEqualTo(BUYER);
            assertThat(saved.getValue().getScheduledAt()).isEqualTo(scheduledAt);
            assertThat(saved.getValue().getReminder30SentAt()).isNull();
            assertThat(product.getStatus()).isEqualTo(ProductStatus.RESERVATION);
        }

        @Test
        @DisplayName("작성자가 방의 구매자면 판매자를 예약자로 하고, 30분 이내 약속은 30분 전 알림을 보낸 것으로 저장한다")
        void it_uses_other_participant_and_marks_near_reminder() {
            when(productPort.lockActive(7L)).thenReturn(Optional.of(product(ProductStatus.ONGOING)));
            when(chatPort.findRoom(9L)).thenReturn(Optional.of(new ChatRoomView(9L, 7L, SELLER, 30L)));

            service().execute(command(SELLER, seoulNow().plusMinutes(10)));

            ArgumentCaptor<ProductReservation> saved = ArgumentCaptor.forClass(ProductReservation.class);
            verify(reservationPort).save(saved.capture());
            assertThat(saved.getValue().getReserverId()).isEqualTo(30L);
            assertThat(saved.getValue().getReminder30SentAt()).isNotNull();
        }

        @Test
        @DisplayName("상품 검사(없음·권한·이미 예약)가 채팅방 검사보다 먼저이며 거절되면 저장·전달하지 않는다")
        void it_rejects_in_order() {
            when(productPort.lockActive(7L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service().execute(command(SELLER, seoulNow()))).isInstanceOf(NotFoundProductException.class);
            when(productPort.lockActive(7L)).thenReturn(Optional.of(product(ProductStatus.ONGOING)));
            assertThatThrownBy(() -> service().execute(command(BUYER, seoulNow()))).isInstanceOf(ForbiddenProductException.class);
            when(productPort.lockActive(7L)).thenReturn(Optional.of(product(ProductStatus.RESERVATION)));
            assertThatThrownBy(() -> service().execute(command(SELLER, seoulNow()))).isInstanceOf(ProductAlreadyReservationException.class);
            verifyNoInteractions(chatPort, reservationPort, tradeStatusPort);
        }

        @Test
        @DisplayName("방이 없거나 다른 상품의 방이면 NOT_FOUND_CHAT_ROOM 이며 저장하지 않는다")
        void it_rejects_wrong_room() {
            when(productPort.lockActive(7L)).thenAnswer(inv -> Optional.of(product(ProductStatus.ONGOING)));
            when(chatPort.findRoom(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service().execute(command(SELLER, seoulNow())))
                    .isInstanceOf(ReservationChatRoomNotFoundException.class);
            when(chatPort.findRoom(9L)).thenReturn(Optional.of(new ChatRoomView(9L, 8L, BUYER, SELLER)));
            assertThatThrownBy(() -> service().execute(command(SELLER, seoulNow())))
                    .isInstanceOf(ReservationChatRoomNotFoundException.class);
            verifyNoInteractions(reservationPort, tradeStatusPort);
            verify(productPort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("예약 취소는")
    class Describe_cancel {

        private CancelReservationService service() {
            return new CancelReservationService(productPort, reservationPort, chatPort, memberPort, tradeStatusPort,
                    notificationPort);
        }

        @Test
        @DisplayName("예약자가 취소하면 상품 → 예약 순으로 잠그고 저장한 뒤 시스템 메시지(id=-예약ID)·알림·상태 전달을 한다")
        void it_cancels_by_reserver() {
            Product product = product(ProductStatus.RESERVATION);
            ProductReservation reservation = pending(seoulNow().plusDays(1), null);
            when(productPort.lockActive(7L)).thenReturn(Optional.of(product));
            when(reservationPort.findPendingForUpdate(7L)).thenReturn(Optional.of(reservation));
            when(chatPort.findRoom(7L, BUYER)).thenReturn(Optional.of(new ChatRoomView(9L, 7L, BUYER, SELLER)));
            when(memberPort.nicknameOf(BUYER)).thenReturn("구매자");
            when(chatPort.saveSystemMessage(eq(9L), eq(-4L), eq(BUYER), eq("구매자님이 예약을 취소했어요"), any()))
                    .thenReturn(-4L);

            service().execute(BUYER, 7L);

            InOrder order = inOrder(productPort, reservationPort, chatPort, notificationPort, tradeStatusPort);
            order.verify(productPort).lockActive(7L);
            order.verify(reservationPort).findPendingForUpdate(7L);
            order.verify(reservationPort).save(reservation);
            order.verify(productPort).save(product);
            order.verify(chatPort).saveSystemMessage(eq(9L), eq(-4L), eq(BUYER), anyString(), any());
            ArgumentCaptor<ReservationCancellation> notified = ArgumentCaptor.forClass(ReservationCancellation.class);
            order.verify(notificationPort).notifyCancelled(notified.capture());
            order.verify(tradeStatusPort).publishChanged(9L, 7L, BUYER, SELLER);
            assertThat(notified.getValue().recipientId()).isEqualTo(SELLER);
            assertThat(notified.getValue().messageId()).isEqualTo(-4L);
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ONGOING);
        }

        @Test
        @DisplayName("작성자도 취소할 수 있고, 채팅방이 없으면 메시지·전달 없이 상태만 저장한다")
        void it_cancels_by_owner_without_room() {
            when(productPort.lockActive(7L)).thenReturn(Optional.of(product(ProductStatus.RESERVATION)));
            when(reservationPort.findPendingForUpdate(7L)).thenReturn(Optional.of(pending(seoulNow(), null)));
            when(chatPort.findRoom(7L, BUYER)).thenReturn(Optional.empty());

            service().execute(SELLER, 7L);

            verify(reservationPort).save(any());
            verify(productPort).save(any());
            verify(chatPort, never()).saveSystemMessage(anyLong(), anyLong(), anyLong(), anyString(), any());
            verifyNoInteractions(notificationPort, tradeStatusPort, memberPort);
        }

        @Test
        @DisplayName("상품이 없으면 NOT_FOUND_PRODUCT, 예약이 없거나 참여자가 아니면 ReservationParticipantOnly 이며 저장하지 않는다")
        void it_rejects() {
            when(productPort.lockActive(7L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service().execute(BUYER, 7L)).isInstanceOf(NotFoundProductException.class);

            when(productPort.lockActive(7L)).thenReturn(Optional.of(product(ProductStatus.RESERVATION)));
            when(reservationPort.findPendingForUpdate(7L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service().execute(BUYER, 7L)).isInstanceOf(ReservationParticipantOnlyException.class);

            when(reservationPort.findPendingForUpdate(7L)).thenReturn(Optional.of(pending(seoulNow(), null)));
            assertThatThrownBy(() -> service().execute(99L, 7L)).isInstanceOf(ReservationParticipantOnlyException.class);

            verify(reservationPort, never()).save(any());
            verify(productPort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("reminder 는")
    class Describe_reminder {

        private ReservationReminderService service() {
            return new ReservationReminderService(reservationPort, productPort, chatPort, memberPort, notificationPort);
        }

        private void given(ProductReservation reservation) {
            when(reservationPort.findProductIdOf(4L)).thenReturn(Optional.of(7L));
            when(productPort.lockActive(7L)).thenReturn(Optional.of(product(ProductStatus.RESERVATION)));
            when(reservationPort.lock(4L)).thenReturn(Optional.of(reservation));
        }

        @Test
        @DisplayName("기한 조회는 now 와 now+30분을 넘긴다")
        void it_queries_due_ids() {
            LocalDateTime now = LocalDateTime.of(2026, 10, 14, 12, 0);
            service().query(now);
            verify(reservationPort).findDueReminderIds(now, now.plusMinutes(30));
        }

        @Test
        @DisplayName("두 알림이 모두 기한이면 양쪽 참여자에게 30분 전 → 정시 순으로 보내고 둘 다 기록한다")
        void it_sends_both_and_marks() {
            ProductReservation reservation = pending(seoulNow().minusMinutes(1), null);
            given(reservation);
            when(chatPort.findRoom(7L, BUYER)).thenReturn(Optional.of(new ChatRoomView(3L, 7L, BUYER, SELLER)));
            when(memberPort.nicknameOf(SELLER)).thenReturn("판매자");
            when(memberPort.nicknameOf(BUYER)).thenReturn("구매자");

            service().execute(4L);

            InOrder order = inOrder(notificationPort, reservationPort);
            order.verify(notificationPort).sendReminder(BUYER, "시민화폐, 광산", "판매자님과의 거래 약속이 30분 뒤에 있어요.", 3L);
            order.verify(notificationPort).sendReminder(SELLER, "시민화폐, 광산", "구매자님과의 거래 약속이 30분 뒤에 있어요.", 3L);
            order.verify(notificationPort).sendReminder(BUYER, "시민화폐, 광산", "판매자님과 약속한 거래 시간이 되었어요.", 3L);
            order.verify(notificationPort).sendReminder(SELLER, "시민화폐, 광산", "구매자님과 약속한 거래 시간이 되었어요.", 3L);
            order.verify(reservationPort).save(reservation);
            assertThat(reservation.getReminder30SentAt()).isNotNull();
            assertThat(reservation.getReminderAtSentAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 보낸 30분 전 알림은 다시 보내지 않는다")
        void it_skips_sent_reminder() {
            LocalDateTime sent = seoulNow().minusMinutes(20);
            ProductReservation reservation = pending(seoulNow().plusMinutes(10), sent);
            given(reservation);
            when(chatPort.findRoom(7L, BUYER)).thenReturn(Optional.of(new ChatRoomView(3L, 7L, BUYER, SELLER)));

            service().execute(4L);

            verifyNoInteractions(notificationPort);
            assertThat(reservation.getReminder30SentAt()).isEqualTo(sent);
            assertThat(reservation.getReminderAtSentAt()).isNull();
        }

        @Test
        @DisplayName("발송이 실패하면 예외를 전파하고 sentAt 을 저장하지 않는다")
        void it_propagates_send_failure_without_saving() {
            ProductReservation reservation = pending(seoulNow().plusMinutes(10), null);
            given(reservation);
            when(chatPort.findRoom(7L, BUYER)).thenReturn(Optional.of(new ChatRoomView(3L, 7L, BUYER, SELLER)));
            when(memberPort.nicknameOf(anyLong())).thenReturn("상대");
            IllegalStateException failure = new IllegalStateException("Expo rejected");
            org.mockito.Mockito.doAnswer(inv -> {
                if (SELLER.equals(inv.getArgument(0))) throw failure;
                return null;
            }).when(notificationPort).sendReminder(anyLong(), anyString(), anyString(), anyLong());

            assertThatThrownBy(() -> service().execute(4L)).isSameAs(failure);

            verify(notificationPort, times(2)).sendReminder(anyLong(), anyString(), anyString(), anyLong());
            verify(reservationPort, never()).save(any());
            assertThat(reservation.getReminder30SentAt()).isNull();
        }

        @Test
        @DisplayName("예약·상품·채팅방이 없거나 PENDING 이 아니면 아무 것도 보내지 않는다")
        void it_skips_inapplicable() {
            when(reservationPort.findProductIdOf(4L)).thenReturn(Optional.empty());
            service().execute(4L);

            when(reservationPort.findProductIdOf(5L)).thenReturn(Optional.of(7L));
            when(productPort.lockActive(7L)).thenReturn(Optional.empty());
            service().execute(5L);

            ProductReservation cancelled = new ProductReservation(6L, 8L, BUYER, ReservationStatus.CANCELLED,
                    seoulNow(), null, null, null, null, null, null);
            when(reservationPort.findProductIdOf(6L)).thenReturn(Optional.of(8L));
            when(productPort.lockActive(8L)).thenReturn(Optional.of(product(ProductStatus.ONGOING)));
            when(reservationPort.lock(6L)).thenReturn(Optional.of(cancelled));
            service().execute(6L);

            given(pending(seoulNow(), null));
            when(chatPort.findRoom(7L, BUYER)).thenReturn(Optional.empty());
            service().execute(4L);

            verifyNoInteractions(notificationPort);
            verify(reservationPort, never()).save(any());
        }
    }
}
