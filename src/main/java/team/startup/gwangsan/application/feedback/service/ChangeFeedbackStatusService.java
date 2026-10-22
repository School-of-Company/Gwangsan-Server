package team.startup.gwangsan.application.feedback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.feedback.data.ChangeFeedbackStatusCommand;
import team.startup.gwangsan.application.feedback.port.in.ChangeFeedbackStatusUseCase;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort.FeedbackAccess;
import team.startup.gwangsan.application.feedback.port.out.SaveFeedbackPort;
import team.startup.gwangsan.domain.feedback.exception.NotFoundFeedbackException;
import team.startup.gwangsan.domain.feedback.model.Feedback;

@Service
@RequiredArgsConstructor
public class ChangeFeedbackStatusService implements ChangeFeedbackStatusUseCase {

    private final LoadFeedbackPort loadFeedbackPort;
    private final SaveFeedbackPort saveFeedbackPort;
    private final FeedbackAdminAccessGuard accessGuard;

    @Override
    @Transactional
    public void execute(ChangeFeedbackStatusCommand command) {
        FeedbackAccess access = loadFeedbackPort.findAccess(command.feedbackId())
                .orElseThrow(NotFoundFeedbackException::new);
        accessGuard.validate(command.actor(), access.type(), access.placeId());

        Feedback feedback = loadFeedbackPort.findById(command.feedbackId()).orElseThrow(NotFoundFeedbackException::new);
        feedback.changeStatus(command.status());
        saveFeedbackPort.changeStatus(feedback);
    }
}
