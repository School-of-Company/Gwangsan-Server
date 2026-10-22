package team.startup.gwangsan.application.feedback.data;

import java.util.List;
import team.startup.gwangsan.domain.feedback.model.*;
import team.startup.gwangsan.domain.feedback.exception.InvalidFeedbackException;

public record CreateFeedbackCommand(Long memberId, FeedbackType type, String content, List<Long> imageIds,
                                    String appVersion, OsType osType, String osVersion, String deviceModel) {
    public CreateFeedbackCommand {
        if (memberId == null || type == null || content == null || content.isBlank() || content.length() > 1000
                || osType == null || imageIds == null || imageIds.size() > 5
                || imageIds.stream().anyMatch(id -> id == null || id <= 0)
                || tooLong(appVersion) || tooLong(osVersion) || tooLong(deviceModel)) {
            throw new InvalidFeedbackException();
        }
        imageIds = imageIds.stream().distinct().toList();
    }

    private static boolean tooLong(String value) {
        return value != null && value.length() > 255;
    }
}
