package team.startup.gwangsan.adapter.integration.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team.startup.gwangsan.adapter.persistence.notification.DeviceToken;
import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.application.notification.port.in.SendPushUseCase;
import team.startup.gwangsan.domain.notification.exception.PushDeliveryFailedException;
import team.startup.gwangsan.domain.notification.model.NotificationType;
import team.startup.gwangsan.domain.notification.model.PushMessage;
import team.startup.gwangsan.global.event.SendNotificationEvent;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("NotificationPort 호출의 제목·본문·유형·sourceId 를 그대로 넘긴다")
    void it_bridges_notification_port() {
        new LegacyNotificationPortBridge(sendPushUseCase).sendNotification(
                List.of(token(3L, "a")), "시민화폐, 광산", "상대님과의 거래 약속이 30분 뒤에 있어요.",
                NotificationType.RESERVATION_REMINDER, 11L);

        ArgumentCaptor<SendPushCommand> captor = ArgumentCaptor.forClass(SendPushCommand.class);
        verify(sendPushUseCase).send(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new SendPushCommand(List.of(3L), new PushMessage(
                "시민화폐, 광산", "상대님과의 거래 약속이 30분 뒤에 있어요.", NotificationType.RESERVATION_REMINDER, 11L)));
    }

    @Test
    @DisplayName("리마인더 발송 실패 예외를 삼키지 않고 호출자에게 전달한다")
    void it_propagates_reminder_failure() {
        when(sendPushUseCase.send(any())).thenThrow(new PushDeliveryFailedException(NotificationType.RESERVATION_REMINDER, 1L));

        assertThatThrownBy(() -> new LegacyNotificationPortBridge(sendPushUseCase).sendNotification(
                List.of(token(3L, "a")), "t", "b", NotificationType.RESERVATION_REMINDER, 1L))
                .isInstanceOf(PushDeliveryFailedException.class);
    }

    @Test
    @DisplayName("실제 SendPushService 와 연결해도 리마인더 최종 실패는 전파되고 예약 취소 실패는 전파되지 않는다")
    void it_propagates_only_reminder_failure_end_to_end() {
        SendPushUseCase real = new team.startup.gwangsan.application.notification.service.SendPushService(
                memberIds -> List.of("ExponentPushToken[a]"),
                (message, tokens) -> team.startup.gwangsan.domain.notification.model.PushOutcome.FAILED);
        LegacyNotificationPortBridge bridge = new LegacyNotificationPortBridge(real);

        assertThatThrownBy(() -> bridge.sendNotification(List.of(token(3L, "a")), "시민화폐, 광산", "b",
                NotificationType.RESERVATION_REMINDER, 1L)).isInstanceOf(PushDeliveryFailedException.class);
        bridge.sendNotification(List.of(token(3L, "a")), "시민화폐, 광산", "b", NotificationType.RESERVATION_CANCEL, 1L);
    }

    @Test
    @DisplayName("토큰 목록이 null 이면 빈 수신자로 호출한다")
    void it_handles_null_tokens() {
        new LegacyNotificationPortBridge(sendPushUseCase).sendNotification(null, "t", "b", NotificationType.RESERVATION_CANCEL, 1L);

        verify(sendPushUseCase).send(new SendPushCommand(List.of(), new PushMessage("t", "b", NotificationType.RESERVATION_CANCEL, 1L)));
    }
}
