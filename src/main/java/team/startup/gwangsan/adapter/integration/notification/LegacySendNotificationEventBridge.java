package team.startup.gwangsan.adapter.integration.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.application.notification.port.in.SendPushUseCase;
import team.startup.gwangsan.domain.notification.model.PushMessage;
import team.startup.gwangsan.global.event.SendNotificationEvent;

/**
 * 임시 bridge: 기존 발행자(거래 확정 PR07, 채팅 PR08, 공지 PR10)가 아직 기기 토큰 목록을 담아 발행한다.
 * 토큰에서 회원 ID 만 꺼내 발송 계약으로 넘기고, 토큰은 커밋 후 notification 이 다시 조회한다.
 * 세 발행자가 PushRequestedEvent 로 바뀌면 SendNotificationEvent 와 함께 삭제한다 (PR10).
 */
@Component
@RequiredArgsConstructor
public class LegacySendNotificationEventBridge {

    private final SendPushUseCase sendPushUseCase;

    @Async("asyncExecutor")
    @TransactionalEventListener(value = SendNotificationEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotification(SendNotificationEvent event) {
        sendPushUseCase.send(new SendPushCommand(
                LegacyDeviceTokens.memberIds(event.deviceTokens()),
                PushMessage.byType(event.type(), event.sourceId())
        ));
    }
}
