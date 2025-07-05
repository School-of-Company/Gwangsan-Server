package team.startup.gwangsan.global.event.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.service.SendNotificationService;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.global.event.NoticeCreatedEvent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NoticeCreatedEventListener {

    private final MemberDetailRepository memberDetailRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final SendNotificationService sendNotificationService;

    @Async("asyncExecutor")
    @TransactionalEventListener(value = NoticeCreatedEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NoticeCreatedEvent event) {

        Notice notice = event.notice();
        List<Long> userIds;

        if (notice.getPlace() == null) {
            userIds = memberDetailRepository.findAll()
                    .stream()
                    .map(md -> md.getMember().getId())
                    .collect(Collectors.toList());
        } else {
            userIds = memberDetailRepository.findAllByPlace(notice.getPlace())
                    .stream()
                    .map(md -> md.getMember().getId())
                    .collect(Collectors.toList());
        }

        List<String> deviceTokens = userIds.stream()
                .map(String::valueOf)
                .map(deviceTokenRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(DeviceToken::getDeviceToken)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!deviceTokens.isEmpty()) {
            sendNotificationService.execute(deviceTokens, NotificationType.NOTICE);
        }
    }
}
