package team.startup.gwangsan.domain.post.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.notification.NotificationPort;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationReminderSenderTest {
    @Mock ProductReservationRepository reservations;
    @Mock ProductRepository products;
    @Mock ChatRoomRepository rooms;
    @Mock DeviceTokenRepository deviceTokens;
    @Mock NotificationPort notificationPort;
    @InjectMocks ReservationReminderSender sender;

    @Test
    void sendsBothRemindersToBothParticipantsOnce() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        Member buyer = member(1L, "구매자");
        Member seller = member(2L, "판매자");
        ProductReservation reservation = ProductReservation.builder()
                .reserver(buyer).status(ReservationStatus.PENDING)
                .scheduledAt(now.minusMinutes(1)).build();
        ReflectionTestUtils.setField(reservation, "createdAt", now.minusHours(1));
        ChatRoom room = ChatRoom.builder().buyer(buyer).seller(seller).build();
        ReflectionTestUtils.setField(room, "id", 3L);
        when(reservations.findProductIdById(4L)).thenReturn(Optional.of(5L));
        when(products.findByIdWithLock(5L)).thenReturn(Optional.of(mock(Product.class)));
        when(reservations.findByIdForUpdate(4L)).thenReturn(Optional.of(reservation));
        when(rooms.findByProductIdAndMember(5L, buyer)).thenReturn(Optional.of(room));
        when(deviceTokens.findAllByUserId(anyLong())).thenReturn(List.of());

        sender.send(4L);
        sender.send(4L);

        verify(notificationPort).sendNotification(anyList(), eq("시민화폐, 광산"),
                eq("판매자님과의 거래 약속이 30분 뒤에 있어요."), eq(NotificationType.RESERVATION_REMINDER), eq(3L));
        verify(notificationPort).sendNotification(anyList(), eq("시민화폐, 광산"),
                eq("구매자님과 약속한 거래 시간이 되었어요."), eq(NotificationType.RESERVATION_REMINDER), eq(3L));
        verify(notificationPort, times(4)).sendNotification(anyList(), anyString(), anyString(), any(), eq(3L));
        assertNotNull(reservation.getReminder30SentAt());
        assertNotNull(reservation.getReminderAtSentAt());
    }

    @Test
    void cancelledReservationCannotSend() {
        ProductReservation reservation = ProductReservation.builder().status(ReservationStatus.CANCELLED).build();
        when(reservations.findProductIdById(4L)).thenReturn(Optional.of(5L));
        when(products.findByIdWithLock(5L)).thenReturn(Optional.of(mock(Product.class)));
        when(reservations.findByIdForUpdate(4L)).thenReturn(Optional.of(reservation));

        sender.send(4L);

        verifyNoInteractions(notificationPort, rooms);
    }

    private Member member(Long id, String nickname) {
        Member member = mock(Member.class);
        lenient().when(member.getId()).thenReturn(id);
        lenient().when(member.getNickname()).thenReturn(nickname);
        return member;
    }
}
