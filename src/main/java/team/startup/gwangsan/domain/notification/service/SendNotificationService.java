package team.startup.gwangsan.domain.notification.service;

import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.util.List;

public interface SendNotificationService {
    void execute(List<String> deviceTokens, NotificationType type, Long sourceId);
}
