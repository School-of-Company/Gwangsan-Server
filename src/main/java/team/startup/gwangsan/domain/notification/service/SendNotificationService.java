package team.startup.gwangsan.domain.notification.service;

import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.util.List;

public interface SendNotificationService {
    void execute(List<DeviceToken> deviceTokens, NotificationType type, Long sourceId);
}
