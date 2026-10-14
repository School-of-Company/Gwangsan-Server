package team.startup.gwangsan.application.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.application.notification.port.out.LoadPushTokensPort;
import team.startup.gwangsan.application.notification.port.out.PushSenderPort;
import team.startup.gwangsan.domain.notification.exception.PushDeliveryFailedException;
import team.startup.gwangsan.domain.notification.model.NotificationType;
import team.startup.gwangsan.domain.notification.model.PushMessage;
import team.startup.gwangsan.domain.notification.model.PushOutcome;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendPushService")
class SendPushServiceTest {

    @Mock
    private LoadPushTokensPort loadPushTokensPort;
    @Mock
    private PushSenderPort pushSenderPort;
    @InjectMocks
    private SendPushService service;

    @Nested
    @DisplayName("send() 는")
    class Describe_send {

        @Test
        @DisplayName("회원 ID 로 토큰을 조회해 발송 Port 에 넘긴다")
        void it_loads_tokens_by_member_ids() {
            PushMessage message = PushMessage.byType(NotificationType.CHATTING, 5L);
            when(loadPushTokensPort.loadTokens(List.of(1L, 2L))).thenReturn(List.of("a", "b"));
            when(pushSenderPort.send(message, List.of("a", "b"))).thenReturn(PushOutcome.SENT);

            assertThat(service.send(new SendPushCommand(List.of(1L, 2L), message))).isEqualTo(PushOutcome.SENT);
        }

        @Test
        @DisplayName("예약 리마인더가 최종 실패하면 PushDeliveryFailedException 을 던진다 (sentAt 미기록 계약)")
        void it_throws_for_failed_reminder() {
            when(loadPushTokensPort.loadTokens(any())).thenReturn(List.of("a"));
            when(pushSenderPort.send(any(), any())).thenReturn(PushOutcome.FAILED);
            PushMessage reminder = new PushMessage("시민화폐, 광산", "본문", NotificationType.RESERVATION_REMINDER, 3L);

            assertThatThrownBy(() -> service.send(new SendPushCommand(List.of(1L), reminder)))
                    .isInstanceOf(PushDeliveryFailedException.class);
        }

        @Test
        @DisplayName("예약 취소·일반 푸시의 실패는 결과로만 돌려준다")
        void it_returns_failure_for_other_types() {
            when(loadPushTokensPort.loadTokens(any())).thenReturn(List.of("a"));
            when(pushSenderPort.send(any(), any())).thenReturn(PushOutcome.FAILED);

            assertThat(service.send(new SendPushCommand(List.of(1L),
                    new PushMessage("t", "b", NotificationType.RESERVATION_CANCEL, 3L)))).isEqualTo(PushOutcome.FAILED);
            assertThat(service.send(new SendPushCommand(List.of(1L),
                    PushMessage.byType(NotificationType.NOTICE, 3L)))).isEqualTo(PushOutcome.FAILED);
        }

        @Test
        @DisplayName("수신자가 없으면 빈 토큰 목록으로 Port 를 호출하고 리마인더도 실패로 보지 않는다")
        void it_treats_no_target_as_non_failure() {
            when(loadPushTokensPort.loadTokens(List.of())).thenReturn(List.of());
            when(pushSenderPort.send(any(), any())).thenReturn(PushOutcome.NO_TARGET);
            PushMessage reminder = new PushMessage("t", "b", NotificationType.RESERVATION_REMINDER, 3L);

            assertThat(service.send(new SendPushCommand(null, reminder))).isEqualTo(PushOutcome.NO_TARGET);
            verify(pushSenderPort).send(reminder, List.of());
        }
    }
}
