package team.startup.gwangsan.global.event.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.alert.service.CreateAlertMembersService;
import team.startup.gwangsan.global.event.CreateAlertMembersEvent;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAlertMembersEventListener 단위 테스트")
class CreateAlertMembersEventListenerTest {

    @InjectMocks private CreateAlertMembersEventListener listener;
    @Mock private CreateAlertMembersService createAlertMembersService;

    @Nested
    @DisplayName("handleCreateAlertMembersEvent() 메서드는")
    class Describe_handleCreateAlertMembersEvent {

        @Nested
        @DisplayName("이벤트를 전달받았을 때")
        class Context_with_event {

            @Test
            @DisplayName("createAlertMembersService.execute()를 올바른 인자로 호출한다")
            void it_calls_execute_with_correct_args() {
                Long sourceId = 1L;
                List<Long> memberIds = List.of(2L, 3L);
                AlertType alertType = AlertType.values()[0];
                CreateAlertMembersEvent event = new CreateAlertMembersEvent(sourceId, memberIds, alertType);

                listener.handleCreateAlertMembersEvent(event);

                verify(createAlertMembersService).execute(sourceId, memberIds, alertType);
            }
        }
    }
}