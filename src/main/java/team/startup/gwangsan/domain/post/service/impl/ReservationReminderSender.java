package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.notification.NotificationPort;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ReservationReminderSender {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final ProductReservationRepository reservations;
    private final ProductRepository products;
    private final ChatRoomRepository rooms;
    private final DeviceTokenRepository deviceTokens;
    private final NotificationPort notificationPort;

    @Transactional
    public void send(Long reservationId) {
        Long productId = reservations.findProductIdById(reservationId).orElse(null);
        if (productId == null || products.findByIdWithLock(productId).isEmpty()) return;
        ProductReservation reservation = reservations.findByIdForUpdate(reservationId).orElse(null);
        if (reservation == null || reservation.getStatus() != ReservationStatus.PENDING) return;
        LocalDateTime now = LocalDateTime.now(SEOUL);
        ChatRoom room = rooms.findByProductIdAndMember(productId, reservation.getReserver())
                .orElse(null);
        if (room == null) return;

        if (reservation.getReminder30SentAt() == null && !now.isBefore(reservation.getScheduledAt().minusMinutes(30))) {
            push(room, "님과의 거래 약속이 30분 뒤에 있어요.");
            reservation.markReminder30Sent(now);
        }
        if (reservation.getReminderAtSentAt() == null && !now.isBefore(reservation.getScheduledAt())) {
            push(room, "님과 약속한 거래 시간이 되었어요.");
            reservation.markReminderAtSent(now);
        }
    }

    private void push(ChatRoom room, String suffix) {
        sendTo(room.getBuyer(), room.getSeller(), room.getId(), suffix);
        sendTo(room.getSeller(), room.getBuyer(), room.getId(), suffix);
    }

    private void sendTo(Member recipient, Member counterpart, Long roomId, String suffix) {
        notificationPort.sendNotification(deviceTokens.findAllByUserId(recipient.getId()),
                "시민화폐, 광산", counterpart.getNickname() + suffix,
                NotificationType.RESERVATION_REMINDER, roomId);
    }
}
