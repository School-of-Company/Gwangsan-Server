package team.startup.gwangsan.adapter.integration.post;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.application.notification.port.in.SendPushUseCase;
import team.startup.gwangsan.domain.notification.model.NotificationType;
import team.startup.gwangsan.domain.notification.model.PushMessage;
import team.startup.gwangsan.global.chat.notification.ChattingServerSystemMessageNotifier;

import java.util.List;

/**
 * 예약 취소 커밋 후 푸시와 채팅 서버 시스템 메시지를 비동기로 보낸다. 실패는 기록만 한다.
 * ponytail: Port 를 구현하는 bean 에 @Async 메서드를 두면 JDK proxy 에 메서드가 없어 기동이 실패하므로 클래스를 나눴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCancelledEventListener {

    private final SendPushUseCase sendPushUseCase;
    private final ChattingServerSystemMessageNotifier chatNotifier;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ReservationCancelledEvent event) {
        try {
            // 제목 "시민화폐, 광산" 은 PushMessage.reservation 이 정한다(기존과 같음).
            sendPushUseCase.send(new SendPushCommand(List.of(event.recipientId()),
                    PushMessage.reservation(NotificationType.RESERVATION_CANCEL, event.roomId(), event.content() + ".")));
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
