package team.startup.gwangsan.global.event.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.domain.notification.NotificationPort;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.global.chat.notification.ChattingServerSystemMessageNotifier;
import team.startup.gwangsan.global.event.ReservationCancelledEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCancelledEventListener {
    private final DeviceTokenRepository deviceTokens;
    private final NotificationPort notificationPort;
    private final ChattingServerSystemMessageNotifier chatNotifier;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ReservationCancelledEvent event) {
        try {
            notificationPort.sendNotification(deviceTokens.findAllByUserId(event.recipientId()),
                    "시민화폐, 광산", event.content() + ".", NotificationType.RESERVATION_CANCEL, event.roomId());
        } catch (Exception e) {
            log.error("Reservation cancellation push failed. roomId={}", event.roomId(), e);
        }
        try {
            chatNotifier.notifySystemMessage(event);
        } catch (Exception e) {
            log.error("Reservation system message delivery failed. roomId={}", event.roomId(), e);
        }
    }
}
