package team.startup.gwangsan.adapter.event.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.application.notification.port.in.SendPushUseCase;
import team.startup.gwangsan.domain.notification.model.NotificationType;
import team.startup.gwangsan.domain.notification.model.PushMessage;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("PushRequestedEventListener")
class PushRequestedEventListenerTest {

    @Test
    @DisplayName("회원 ID 와 유형별 문구로 발송 계약을 호출한다")
    void it_sends_by_member_ids() {
        SendPushUseCase useCase = mock(SendPushUseCase.class);

        new PushRequestedEventListener(useCase).handle(new PushRequestedEvent(List.of(1L), NotificationType.NOTICE, 3L));

        verify(useCase).send(new SendPushCommand(List.of(1L), PushMessage.byType(NotificationType.NOTICE, 3L)));
    }
}
