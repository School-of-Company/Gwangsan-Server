package team.startup.gwangsan.domain.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.notification.NotificationPort;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.service.SendNotificationService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendNotificationServiceImpl implements SendNotificationService {

    private final NotificationPort notificationPort;

    private static final String GWANGSAN_DEFAULT_TITLE = "시민화폐 광산";

    private static final String NOTICE_BODY = "새로운 공지가 등록되었습니다.";
    private static final String CHAT_BODY = "새로운 메세지가 도착했습니다.";
    private static final String RECOMMENDATION_BODY = "추천인이 등록되었습니다.";
    private static final String TRADE_COMPLETE_BODY = "거래가 승인되었습니다.";

    @Override
    @Transactional
    public void execute(List<String> deviceTokens, NotificationType type) {
        notificationPort.sendNotification(
                deviceTokens,
                GWANGSAN_DEFAULT_TITLE,
                createBodyByType(type)
        );
    }

    private String createBodyByType(NotificationType type) {
        return switch (type) {
            case NOTICE -> NOTICE_BODY;
            case CHATTING -> CHAT_BODY;
            case RECOMMENDATION -> RECOMMENDATION_BODY;
            case TRADE_COMPLETE -> TRADE_COMPLETE_BODY;
        };
    }
}
