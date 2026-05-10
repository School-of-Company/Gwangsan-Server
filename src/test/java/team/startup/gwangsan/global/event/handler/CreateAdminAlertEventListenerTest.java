package team.startup.gwangsan.global.event.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.service.CreateAdminAlertService;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAdminAlertEventListener 단위 테스트")
class CreateAdminAlertEventListenerTest {

    @InjectMocks private CreateAdminAlertEventListener listener;
    @Mock private CreateAdminAlertService createAdminAlertService;

    @Nested
    @DisplayName("handleCreateAdminAlertEvent() 메서드는")
    class Describe_handleCreateAdminAlertEvent {

        @Nested
        @DisplayName("이벤트를 전달받았을 때")
        class Context_with_event {

            @Test
            @DisplayName("createAdminAlertService.execute()를 올바른 인자로 호출한다")
            void it_calls_execute_with_correct_args() {
                AlertType type = AlertType.values()[0];
                Long sourceId = 1L;
                Long memberId = 2L;
                CreateAdminAlertEvent event = new CreateAdminAlertEvent(type, sourceId, memberId);

                listener.handleCreateAdminAlertEvent(event);

                verify(createAdminAlertService).execute(type, sourceId, memberId);
            }
        }
    }
}