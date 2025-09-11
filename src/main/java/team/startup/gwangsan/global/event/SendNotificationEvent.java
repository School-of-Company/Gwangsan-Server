package team.startup.gwangsan.global.event;

import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.util.List;

public record SendNotificationEvent(
        List<String> deviceTokens,
        NotificationType type,
        Long sourceId
) {
}
