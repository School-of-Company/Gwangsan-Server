package team.startup.gwangsan.adapter.integration.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.application.notification.port.in.SendPushUseCase;
import team.startup.gwangsan.domain.notification.model.NotificationType;
import team.startup.gwangsan.domain.notification.model.PushMessage;
import team.startup.gwangsan.global.chat.notification.ChattingServerSystemMessageNotifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("예약 취소 커밋 후 알림 listener")
class ReservationCancelledEventListenerTest {

    @Mock SendPushUseCase sendPushUseCase;
    @Mock ChattingServerSystemMessageNotifier chatNotifier;
    @InjectMocks ReservationCancelledEventListener listener;

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 14, 12, 0);

    @Test
    @DisplayName("상대에게 \"시민화폐, 광산\" 제목과 마침표 붙은 본문으로 보내고, 푸시가 실패해도 채팅 서버 전달을 시도하며 예외를 던지지 않는다")
    void it_continues_after_push_failure() {
        ReservationCancelledEvent event = new ReservationCancelledEvent(9L, -4L, 20L, 10L, "구매자님이 예약을 취소했어요", AT);
        when(sendPushUseCase.send(any())).thenThrow(new IllegalStateException("push down"));
        doThrow(new IllegalStateException("chat down")).when(chatNotifier).notifySystemMessage(event);

        listener.handle(event);

        verify(sendPushUseCase).send(new SendPushCommand(List.of(10L),
                new PushMessage("시민화폐, 광산", "구매자님이 예약을 취소했어요.", NotificationType.RESERVATION_CANCEL, 9L)));
        verify(chatNotifier).notifySystemMessage(event);
    }
}
