package team.startup.gwangsan.adapter.integration.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.application.notification.port.in.SendPushUseCase;
import team.startup.gwangsan.application.notification.service.SendPushService;
import team.startup.gwangsan.application.post.data.ReservationCancellation;
import team.startup.gwangsan.domain.notification.exception.PushDeliveryFailedException;
import team.startup.gwangsan.domain.notification.model.NotificationType;
import team.startup.gwangsan.domain.notification.model.PushMessage;
import team.startup.gwangsan.domain.notification.model.PushOutcome;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("예약 알림 bridge")
class ReservationNotificationBridgeAdapterTest {

    @Mock ApplicationEventPublisher eventPublisher;
    @Mock SendPushUseCase sendPushUseCase;
    @InjectMocks ReservationNotificationBridgeAdapter adapter;

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 14, 12, 0);

    @Nested
    @DisplayName("reminder 는")
    class Describe_reminder {

        @Test
        @DisplayName("수신자 회원 ID 로 제목·본문·RESERVATION_REMINDER·roomId 를 그대로 동기 발송한다")
        void it_sends_to_recipient() {
            adapter.sendReminder(20L, "시민화폐, 광산", "본문", 3L);

            verify(sendPushUseCase).send(new SendPushCommand(List.of(20L),
                    new PushMessage("시민화폐, 광산", "본문", NotificationType.RESERVATION_REMINDER, 3L)));
        }

        @Test
        @DisplayName("발송 예외를 삼키지 않고 그대로 던진다(sentAt 미기록 조건)")
        void it_propagates_failure() {
            PushDeliveryFailedException failure = new PushDeliveryFailedException(NotificationType.RESERVATION_REMINDER, 3L);
            when(sendPushUseCase.send(any())).thenThrow(failure);

            assertThatThrownBy(() -> adapter.sendReminder(20L, "t", "b", 3L)).isSameAs(failure);
        }

        @Test
        @DisplayName("실제 SendPushService 와 연결하면 최종 실패는 전파되고 토큰 없음(NO_TARGET)은 성공으로 끝난다")
        void it_keeps_contract_with_real_service() {
            ReservationNotificationBridgeAdapter failing = new ReservationNotificationBridgeAdapter(eventPublisher,
                    new SendPushService(ids -> List.of("ExponentPushToken[a]"), (message, tokens) -> PushOutcome.FAILED));
            ReservationNotificationBridgeAdapter noTarget = new ReservationNotificationBridgeAdapter(eventPublisher,
                    new SendPushService(ids -> List.of(), (message, tokens) -> PushOutcome.NO_TARGET));

            assertThatThrownBy(() -> failing.sendReminder(20L, "시민화폐, 광산", "b", 3L))
                    .isInstanceOf(PushDeliveryFailedException.class);
            noTarget.sendReminder(20L, "시민화폐, 광산", "b", 3L);
        }
    }

    @Nested
    @DisplayName("예약 취소는")
    class Describe_cancel {

        @Test
        @DisplayName("트랜잭션 안에서는 이벤트만 발행한다")
        void it_publishes_event_only() {
            adapter.notifyCancelled(new ReservationCancellation(9L, -4L, 20L, 10L, "구매자님이 예약을 취소했어요", AT));

            verify(eventPublisher).publishEvent(new ReservationCancelledEvent(9L, -4L, 20L, 10L, "구매자님이 예약을 취소했어요", AT));
            verifyNoInteractions(sendPushUseCase);
        }
    }
}
