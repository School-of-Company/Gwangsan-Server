package team.startup.gwangsan.adapter.integration.feedback;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.feedback.port.out.FeedbackReferencesPort;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.image.port.in.ImageQuery;
import team.startup.gwangsan.application.alert.port.in.RecordAlertUseCase;
import team.startup.gwangsan.application.alert.data.RecordAlertCommand;
import team.startup.gwangsan.adapter.event.notification.PushRequestedEvent;
import team.startup.gwangsan.domain.alert.model.Alert;
import team.startup.gwangsan.domain.notification.model.NotificationType;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.feedback.exception.*;

@Component
@RequiredArgsConstructor
public class FeedbackReferencesAdapter implements FeedbackReferencesPort {
    private final MemberSummaryQuery members;
    private final ImageQuery images;
    private final RecordAlertUseCase alerts;
    private final ApplicationEventPublisher events;

    @Override
    public void requireMember(Long memberId) {
        members.findById(memberId).orElseThrow(NotFoundMemberException::new);
    }

    @Override
    public Integer requirePlace(Long memberId) {
        Integer placeId = members.findById(memberId).orElseThrow(NotFoundMemberException::new).placeId();
        if (placeId == null) throw new ForbiddenFeedbackException();
        return placeId;
    }

    @Override
    public void requireImages(List<Long> imageIds) {
        if (images.findAllByIds(imageIds).size() != imageIds.size()) throw new InvalidFeedbackException();
    }

    @Override
    public void notifyAnswered(Long feedbackId, Long memberId, Long adminId) {
        alerts.record(new RecordAlertCommand(Alert.feedbackAnswer(feedbackId, adminId), List.of(memberId)));
        events.publishEvent(new PushRequestedEvent(List.of(memberId), NotificationType.FEEDBACK_ANSWER, feedbackId));
    }
}
