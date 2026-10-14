package team.startup.gwangsan.domain.notification;

import team.startup.gwangsan.adapter.persistence.notification.DeviceToken;
import team.startup.gwangsan.domain.notification.model.NotificationType;

import java.util.List;

/**
 * legacy: 예약 리마인더/취소(PR06)가 전환될 때까지 남긴다. 구현은 LegacyNotificationPortBridge.
 */
public interface NotificationPort {
    void sendNotification(List<DeviceToken> deviceTokens, String title, String body, NotificationType type, Long sourceId);
}
