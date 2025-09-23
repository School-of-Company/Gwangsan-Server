package team.startup.gwangsan.global.event;

import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.util.List;

public record SendNotificationEvent(
        List<DeviceToken> deviceTokens,
        NotificationType type,
        Long sourceId
) {
}
