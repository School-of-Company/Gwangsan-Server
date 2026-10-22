package team.startup.gwangsan.application.feedback.data;

import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.util.List;

public record CreateFeedbackCommand(
        ActorContext actor,
        FeedbackType type,
        String content,
        List<Long> imageIds,
        String appVersion,
        OsType osType,
        String osVersion,
        String deviceModel
) {
}
