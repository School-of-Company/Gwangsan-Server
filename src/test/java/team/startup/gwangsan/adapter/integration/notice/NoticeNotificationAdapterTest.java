package team.startup.gwangsan.adapter.integration.notice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.adapter.event.alert.CreateAlertMembersEvent;
import team.startup.gwangsan.adapter.event.notification.PushRequestedEvent;
import team.startup.gwangsan.domain.alert.model.AlertType;
import team.startup.gwangsan.domain.notification.model.NotificationType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeNotificationAdapter")
class NoticeNotificationAdapterTest {

    @InjectMocks private NoticeNotificationAdapter adapter;

    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @Test
    @DisplayName("회원 ID 로 푸시 요청 이벤트 다음 알림함(async) 이벤트를 발행한다")
    void it_publishes_push_then_alert_events() {
        adapter.notifyCreated(100L, List.of(1L, 2L));

        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(2)).publishEvent(events.capture());
        assertThat(events.getAllValues()).containsExactly(
                new PushRequestedEvent(List.of(1L, 2L), NotificationType.NOTICE, 100L),
                new CreateAlertMembersEvent(100L, List.of(1L, 2L), AlertType.NOTICE));
    }

    @Test
    @DisplayName("수신 회원이 없어도 기존처럼 두 이벤트를 발행한다")
    void it_publishes_events_without_members() {
        adapter.notifyCreated(100L, List.of());

        verify(applicationEventPublisher).publishEvent(new PushRequestedEvent(List.of(), NotificationType.NOTICE, 100L));
        verify(applicationEventPublisher).publishEvent(new CreateAlertMembersEvent(100L, List.of(), AlertType.NOTICE));
    }
}
