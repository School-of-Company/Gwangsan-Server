package team.startup.gwangsan.adapter.web.feedback.dto;

import java.util.List;
import jakarta.validation.constraints.*;
import team.startup.gwangsan.application.feedback.data.CreateFeedbackCommand;
import team.startup.gwangsan.domain.feedback.model.*;

public record CreateFeedbackRequest(
        @NotNull FeedbackType type,
        @NotBlank @Size(max = 1000) String content,
        @NotNull @Size(max = 5) List<@NotNull @Positive Long> imageIds,
        @Size(max = 255) String appVersion,
        @NotNull OsType osType,
        @Size(max = 255) String osVersion,
        @Size(max = 255) String deviceModel) {
    public CreateFeedbackCommand toCommand(Long memberId) {
        return new CreateFeedbackCommand(memberId, type, content, imageIds, appVersion, osType, osVersion, deviceModel);
    }
}
