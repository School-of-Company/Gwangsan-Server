package team.startup.gwangsan.adapter.web.feedback.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import team.startup.gwangsan.application.feedback.data.CreateFeedbackCommand;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.util.List;

public record CreateFeedbackRequest(
        @NotNull FeedbackType type,
        @NotBlank @Size(max = 1000) String content,
        @Size(max = 5) List<Long> imageIds,
        String appVersion,
        OsType osType,
        String osVersion,
        String deviceModel
) {
    public CreateFeedbackCommand toCommand(ActorContext actor) {
        return new CreateFeedbackCommand(actor, type, content, imageIds, appVersion, osType, osVersion, deviceModel);
    }
}
