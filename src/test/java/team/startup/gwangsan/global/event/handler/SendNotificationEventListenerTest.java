package team.startup.gwangsan.global.event.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.service.SendNotificationService;
import team.startup.gwangsan.global.event.SendNotificationEvent;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendNotificationEventListener 단위 테스트")
class SendNotificationEventListenerTest {

    @InjectMocks private SendNotificationEventListener listener;
    @Mock private SendNotificationService sendNotificationService;

    @Nested
    @DisplayName("handleNotification() 메서드는")
    class Describe_handleNotification {

        @Nested
        @DisplayName("이벤트를 전달받았을 때")
        class Context_with_event {

            @Test
            @DisplayName("sendNotificationService.execute()를 올바른 인자로 호출한다")
            void it_calls_execute_with_correct_args() {
                List<DeviceToken> deviceTokens = List.of(mock(DeviceToken.class));
                NotificationType type = NotificationType.values()[0];
                Long sourceId = 1L;
                SendNotificationEvent event = new SendNotificationEvent(deviceTokens, type, sourceId);

                listener.handleNotification(event);

                verify(sendNotificationService).execute(deviceTokens, type, sourceId);
            }
        }
    }
}