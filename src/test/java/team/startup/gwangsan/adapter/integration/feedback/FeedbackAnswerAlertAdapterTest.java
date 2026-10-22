package team.startup.gwangsan.adapter.integration.feedback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.adapter.event.alert.CreateAlertEvent;
import team.startup.gwangsan.adapter.event.notification.PushRequestedEvent;
import team.startup.gwangsan.domain.alert.model.AlertType;
import team.startup.gwangsan.domain.notification.model.NotificationType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackAnswerAlertAdapter 단위 테스트")
class FeedbackAnswerAlertAdapterTest {

    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @Test
    @DisplayName("답변 알림 이벤트와 푸시 요청 이벤트를 둘 다 발행한다")
    void it_publishes_alert_and_push_events() {
        new FeedbackAnswerAlertAdapter(applicationEventPublisher).requestAnswerAlert(5L, 9L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).contains(
                new CreateAlertEvent(5L, 9L, AlertType.FEEDBACK_ANSWER),
                new PushRequestedEvent(List.of(9L), NotificationType.FEEDBACK_ANSWER, 5L));
    }
}
