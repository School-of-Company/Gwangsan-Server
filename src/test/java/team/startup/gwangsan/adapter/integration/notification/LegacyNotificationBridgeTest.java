package team.startup.gwangsan.adapter.integration.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.adapter.persistence.notification.DeviceToken;
import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.application.notification.port.in.SendPushUseCase;
import team.startup.gwangsan.domain.notification.model.NotificationType;
import team.startup.gwangsan.domain.notification.model.PushMessage;
import team.startup.gwangsan.global.event.SendNotificationEvent;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@DisplayName("기존 발송 경로 bridge 는 토큰 대신 회원 ID 로 발송 계약을 호출한다")
class LegacyNotificationBridgeTest {

    private final SendPushUseCase sendPushUseCase = mock(SendPushUseCase.class);

    private static DeviceToken token(Long userId, String value) {
        return DeviceToken.builder().deviceId(value).userId(userId).deviceToken(value).build();
    }

    @Test
    @DisplayName("SendNotificationEvent 의 토큰에서 회원 ID 를 중복 없이 뽑고 유형별 기본 문구를 쓴다")
    void it_bridges_legacy_event() {
        new LegacySendNotificationEventBridge(sendPushUseCase).handleNotification(new SendNotificationEvent(
                Arrays.asList(token(1L, "a"), token(1L, "b"), null, token(2L, "c")), NotificationType.TRADE_COMPLETE, 9L));

        verify(sendPushUseCase).send(new SendPushCommand(List.of(1L, 2L), PushMessage.byType(NotificationType.TRADE_COMPLETE, 9L)));
    }

    @Test
    @DisplayName("토큰 목록이 null 이면 빈 수신자로 호출한다")
    void it_handles_null_tokens() {
        new LegacySendNotificationEventBridge(sendPushUseCase).handleNotification(
                new SendNotificationEvent(null, NotificationType.TRADE_COMPLETE, 1L));

        verify(sendPushUseCase).send(new SendPushCommand(List.of(), PushMessage.byType(NotificationType.TRADE_COMPLETE, 1L)));
    }
}
