package team.startup.gwangsan.adapter.web.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;

public record ChangeFeedbackStatusRequest(@NotNull FeedbackStatus status) {
}
