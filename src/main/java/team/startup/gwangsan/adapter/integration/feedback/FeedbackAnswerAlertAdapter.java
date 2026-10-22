package team.startup.gwangsan.adapter.integration.feedback;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.event.alert.CreateAlertEvent;
import team.startup.gwangsan.adapter.event.notification.PushRequestedEvent;
import team.startup.gwangsan.application.feedback.port.out.RequestFeedbackAnswerAlertPort;
import team.startup.gwangsan.domain.alert.model.AlertType;
import team.startup.gwangsan.domain.notification.model.NotificationType;

import java.util.List;

/** 문의 답변 알림: 알림함 저장과 푸시를 둘 다 커밋 후 비동기로 발행한다. */
@Component
@RequiredArgsConstructor
public class FeedbackAnswerAlertAdapter implements RequestFeedbackAnswerAlertPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void requestAnswerAlert(Long feedbackId, Long memberId) {
        applicationEventPublisher.publishEvent(new CreateAlertEvent(feedbackId, memberId, AlertType.FEEDBACK_ANSWER));
        applicationEventPublisher.publishEvent(
                new PushRequestedEvent(List.of(memberId), NotificationType.FEEDBACK_ANSWER, feedbackId));
    }
}
