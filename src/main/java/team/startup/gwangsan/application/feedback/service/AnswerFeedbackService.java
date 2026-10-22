package team.startup.gwangsan.application.feedback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.feedback.data.AnswerFeedbackCommand;
import team.startup.gwangsan.application.feedback.port.in.AnswerFeedbackUseCase;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort.FeedbackAccess;
import team.startup.gwangsan.application.feedback.port.out.RequestFeedbackAnswerAlertPort;
import team.startup.gwangsan.application.feedback.port.out.SaveFeedbackPort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.exception.NotFoundFeedbackException;
import team.startup.gwangsan.domain.feedback.model.Feedback;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnswerFeedbackService implements AnswerFeedbackUseCase {

    private final LoadFeedbackPort loadFeedbackPort;
    private final SaveFeedbackPort saveFeedbackPort;
    private final RequestFeedbackAnswerAlertPort requestFeedbackAnswerAlertPort;
    private final FeedbackAdminAccessGuard accessGuard;

    @Override
    @Transactional
    public void execute(AnswerFeedbackCommand command) {
        ActorContext actor = command.actor();
        FeedbackAccess access = loadFeedbackPort.findAccess(command.feedbackId())
                .orElseThrow(NotFoundFeedbackException::new);
        accessGuard.validate(actor, access.type(), access.placeId());

        Feedback feedback = loadFeedbackPort.findById(command.feedbackId()).orElseThrow(NotFoundFeedbackException::new);
        feedback.answer(command.answer(), actor.memberId(), LocalDateTime.now());
        saveFeedbackPort.saveAnswer(feedback);

        requestFeedbackAnswerAlertPort.requestAnswerAlert(feedback.getId(), feedback.getMemberId());
    }
}
