package team.startup.gwangsan.domain.notification;

import java.util.List;

public interface NotificationPort {
    void sendNotification(List<String> deviceTokens, String title, String body);
}
