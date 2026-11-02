package team.startup.gwangsan.application.feedback.port.in;

import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;

public interface UpdateFeedbackUseCase {
    void answer(ActorContext actor, Long id, String answer);
    void changeStatus(ActorContext actor, Long id, FeedbackStatus status);
}
