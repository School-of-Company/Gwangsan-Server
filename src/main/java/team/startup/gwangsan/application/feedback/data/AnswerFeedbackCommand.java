package team.startup.gwangsan.application.feedback.data;

import team.startup.gwangsan.domain.common.ActorContext;

public record AnswerFeedbackCommand(ActorContext actor, Long feedbackId, String answer) {
}
