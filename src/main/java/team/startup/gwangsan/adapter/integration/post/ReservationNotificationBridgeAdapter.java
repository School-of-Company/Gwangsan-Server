package team.startup.gwangsan.adapter.integration.post;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.post.data.ReservationCancellation;
import team.startup.gwangsan.application.post.port.out.ReservationNotificationPort;
import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.application.notification.port.in.SendPushUseCase;
import team.startup.gwangsan.domain.notification.model.NotificationType;
import team.startup.gwangsan.domain.notification.model.PushMessage;

import java.util.List;

/**
 * 예약 알림을 notification 공개 {@link SendPushUseCase} 로 보낸다.
 *
 * <p>취소 알림은 이벤트만 발행하고 커밋 후 {@link ReservationCancelledEventListener} 가 보낸다. reminder 는 동기로 보내며
 * 최종 실패 시 SendPushUseCase 가 PushDeliveryFailedException 을 던지므로 호출자의 sentAt 기록이 rollback 된다.
 * 토큰이 없으면 NO_TARGET 으로 끝나 예외가 없다(기존과 같음).
 */
@Component
@RequiredArgsConstructor
public class ReservationNotificationBridgeAdapter implements ReservationNotificationPort {

    private final ApplicationEventPublisher eventPublisher;
    private final SendPushUseCase sendPushUseCase;

    @Override
    public void notifyCancelled(ReservationCancellation cancellation) {
        eventPublisher.publishEvent(new ReservationCancelledEvent(cancellation.roomId(), cancellation.messageId(),
                cancellation.senderId(), cancellation.recipientId(), cancellation.content(), cancellation.createdAt()));
    }

    @Override
    public void sendReminder(Long recipientId, String title, String body, Long roomId) {
        sendPushUseCase.send(new SendPushCommand(List.of(recipientId),
                new PushMessage(title, body, NotificationType.RESERVATION_REMINDER, roomId)));
    }
}
