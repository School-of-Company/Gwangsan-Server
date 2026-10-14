package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.post.data.ChatRoomView;
import team.startup.gwangsan.application.post.port.in.DueReservationReminderQuery;
import team.startup.gwangsan.application.post.port.in.SendReservationReminderUseCase;
import team.startup.gwangsan.application.post.port.out.ProductChatPort;
import team.startup.gwangsan.application.post.port.out.ProductMemberPort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.application.post.port.out.ReservationNotificationPort;
import team.startup.gwangsan.application.post.port.out.ReservationPort;
import team.startup.gwangsan.domain.post.model.ProductReservation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservationReminderService implements DueReservationReminderQuery, SendReservationReminderUseCase {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final String TITLE = "시민화폐, 광산";

    private final ReservationPort reservationPort;
    private final ProductPort productPort;
    private final ProductChatPort chatPort;
    private final ProductMemberPort memberPort;
    private final ReservationNotificationPort notificationPort;

    @Override
    @Transactional(readOnly = true)
    public List<Long> query(LocalDateTime now) {
        return reservationPort.findDueReminderIds(now, now.plusMinutes(30));
    }

    /**
     * 예약 취소·거래 완료와 같은 상품→예약 잠금 순서를 지킨다. 발송은 동기이며 실패하면 예외로
     * rollback 되어 sentAt 이 남지 않는다. 한 호출 안에서 일부 수신자에게 보낸 뒤 실패하면 다음 주기에
     * 같은 알림을 다시 보낸다(기존 동작).
     */
    @Override
    @Transactional
    public void execute(Long reservationId) {
        Optional<Long> productId = reservationPort.findProductIdOf(reservationId);
        if (productId.isEmpty() || productPort.lockActive(productId.get()).isEmpty()) return;
        ProductReservation reservation = reservationPort.lock(reservationId).orElse(null);
        if (reservation == null || !reservation.isPending()) return;

        LocalDateTime now = LocalDateTime.now(SEOUL);
        ChatRoomView room = chatPort.findRoom(productId.get(), reservation.getReserverId()).orElse(null);
        if (room == null) return;

        if (reservation.isReminder30Due(now)) {
            push(room, "님과의 거래 약속이 30분 뒤에 있어요.");
            reservation.markReminder30Sent(now);
        }
        if (reservation.isReminderAtDue(now)) {
            push(room, "님과 약속한 거래 시간이 되었어요.");
            reservation.markReminderAtSent(now);
        }
        reservationPort.save(reservation);
    }

    private void push(ChatRoomView room, String suffix) {
        notificationPort.sendReminder(room.buyerId(), TITLE, memberPort.nicknameOf(room.sellerId()) + suffix, room.roomId());
        notificationPort.sendReminder(room.sellerId(), TITLE, memberPort.nicknameOf(room.buyerId()) + suffix, room.roomId());
    }
}
