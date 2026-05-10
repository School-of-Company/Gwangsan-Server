package team.startup.gwangsan.global.event.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.alert.service.CreateAlertService;
import team.startup.gwangsan.global.event.CreateAlertEvent;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAlertEventListener 단위 테스트")
class CreateAlertEventListenerTest {

    @InjectMocks private CreateAlertEventListener listener;
    @Mock private CreateAlertService createAlertService;

    @Nested
    @DisplayName("handleCreateAlertEvent() 메서드는")
    class Describe_handleCreateAlertEvent {

        @Nested
        @DisplayName("이벤트를 전달받았을 때")
        class Context_with_event {

            @Test
            @DisplayName("createAlertService.execute()를 올바른 인자로 호출한다")
            void it_calls_execute_with_correct_args() {
                Long sourceId = 1L;
                Long memberId = 2L;
                AlertType alertType = AlertType.values()[0];
                Long suspendId = 3L;
                CreateAlertEvent event = new CreateAlertEvent(sourceId, memberId, alertType, suspendId);

                listener.handleCreateAlertEvent(event);

                verify(createAlertService).execute(sourceId, memberId, alertType, suspendId);
            }
        }
    }
}