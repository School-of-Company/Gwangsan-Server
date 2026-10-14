package team.startup.gwangsan.global.event;

import team.startup.gwangsan.adapter.persistence.notification.DeviceToken;
import team.startup.gwangsan.domain.notification.model.NotificationType;

import java.util.List;

public record SendNotificationEvent(
        List<DeviceToken> deviceTokens,
        NotificationType type,
        Long sourceId
) {
}
