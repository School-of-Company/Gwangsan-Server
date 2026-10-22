package team.startup.gwangsan.application.feedback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import team.startup.gwangsan.application.feedback.data.CreateFeedbackCommand;
import team.startup.gwangsan.application.feedback.port.in.CreateFeedbackUseCase;
import team.startup.gwangsan.application.feedback.port.out.*;
import team.startup.gwangsan.domain.feedback.model.*;

@Service
@RequiredArgsConstructor
public class CreateFeedbackService implements CreateFeedbackUseCase {
    private final FeedbackPort feedbackPort;
    private final FeedbackReferencesPort references;

    @Override
    @Transactional
    public void execute(CreateFeedbackCommand command) {
        references.requireMember(command.memberId());
        references.requireImages(command.imageIds());
        LocalDateTime now = LocalDateTime.now();
        feedbackPort.create(new Feedback(null, command.memberId(), command.type(), command.content(),
                command.appVersion(), command.osType(), command.osVersion(), command.deviceModel(),
                FeedbackStatus.RECEIVED, null, null, null, now, now), command.imageIds());
    }
}
