package team.startup.gwangsan.adapter.integration.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.notification.DeviceToken;
import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.application.notification.port.in.SendPushUseCase;
import team.startup.gwangsan.domain.notification.NotificationPort;
import team.startup.gwangsan.domain.notification.model.NotificationType;
import team.startup.gwangsan.domain.notification.model.PushMessage;

import java.util.List;

/**
 * 임시 bridge: 예약 리마인더와 예약 취소 알림(PR06)이 기존 NotificationPort 를 직접 호출한다.
 * 리마인더 최종 실패 시 SendPushUseCase 가 예외를 던져 sentAt 을 남기지 않는 기존 계약을 유지한다.
 * PR06 이 SendPushUseCase 로 전환하면 NotificationPort 와 함께 삭제한다.
 */
@Component
@RequiredArgsConstructor
public class LegacyNotificationPortBridge implements NotificationPort {

    private final SendPushUseCase sendPushUseCase;

    @Override
    public void sendNotification(List<DeviceToken> deviceTokens, String title, String body, NotificationType type, Long sourceId) {
        sendPushUseCase.send(new SendPushCommand(
                LegacyDeviceTokens.memberIds(deviceTokens),
                new PushMessage(title, body, type, sourceId)
        ));
    }
}
