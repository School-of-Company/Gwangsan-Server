package team.startup.gwangsan.application.feedback.port.in;

import team.startup.gwangsan.application.feedback.data.FeedbackDetail;
import team.startup.gwangsan.domain.common.ActorContext;

public interface FindAdminFeedbackQuery {
    /** 문의가 없으면 NotFoundFeedback, PLACE_ADMIN 범위 밖이면 AdminPlaceMismatch. */
    FeedbackDetail query(ActorContext actor, Long feedbackId);
}
