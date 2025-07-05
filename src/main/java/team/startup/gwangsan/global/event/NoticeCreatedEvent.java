package team.startup.gwangsan.global.event;

import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.util.List;

public record NoticeCreatedEvent(
        Notice notice,
        List<String> deviceTokens,
        NotificationType type
) {}