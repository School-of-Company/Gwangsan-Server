package team.startup.gwangsan.adapter.event.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.alert.data.CreateAlertForMembersCommand;
import team.startup.gwangsan.application.alert.data.CreateAlertFromSourceCommand;
import team.startup.gwangsan.application.alert.port.in.CreateAlertFromSourceUseCase;
import team.startup.gwangsan.domain.alert.model.AlertType;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("AlertEventListener")
class AlertEventListenerTest {

    private final CreateAlertFromSourceUseCase useCase = mock(CreateAlertFromSourceUseCase.class);
    private final AlertEventListener listener = new AlertEventListener(useCase);

    @Test
    @DisplayName("CreateAlertEvent 필드를 그대로 command 로 넘긴다")
    void it_maps_create_alert_event() {
        listener.handleCreateAlertEvent(new CreateAlertEvent(1L, 2L, AlertType.REPORT, 3L));

        verify(useCase).create(new CreateAlertFromSourceCommand(AlertType.REPORT, 1L, 2L, 3L));
    }

    @Test
    @DisplayName("CreateAlertMembersEvent 필드를 그대로 command 로 넘긴다")
    void it_maps_members_event() {
        listener.handleCreateAlertMembersEvent(new CreateAlertMembersEvent(1L, List.of(2L, 3L), AlertType.NOTICE));

        verify(useCase).createForMembers(new CreateAlertForMembersCommand(AlertType.NOTICE, 1L, List.of(2L, 3L)));
    }
}
