package team.startup.gwangsan.domain.notification;

import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.util.List;

public interface NotificationPort {
    void sendNotification(List<String> deviceTokens, String title, String body, NotificationType type, Long sourceId);
}
