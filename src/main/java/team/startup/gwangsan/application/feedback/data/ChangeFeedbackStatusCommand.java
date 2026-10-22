package team.startup.gwangsan.application.feedback.data;

import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;

public record ChangeFeedbackStatusCommand(ActorContext actor, Long feedbackId, FeedbackStatus status) {
}
