package team.startup.gwangsan.adapter.integration.notice;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.event.alert.CreateAlertMembersEvent;
import team.startup.gwangsan.adapter.event.notification.PushRequestedEvent;
import team.startup.gwangsan.application.notice.port.out.NoticeNotificationPort;
import team.startup.gwangsan.domain.alert.model.AlertType;
import team.startup.gwangsan.domain.notification.model.NotificationType;

import java.util.List;

/**
 * 공지 등록 푸시와 알림함 기록을 커밋 후 비동기 이벤트로 발행한다(발행 순서: 푸시 → 알림함).
 * 공지 알림함은 원자 저장 대상(OTHER_MEMBER_TRADE_COMPLETE, TRADE_CANCEL)이 아니므로
 * 계획 확정안(1차 구현 검토 확정: 알림 저장 범위)대로 AFTER_COMMIT async 를 유지한다.
 * 푸시는 회원 ID 만 넘기고 기기 토큰은 커밋 후 notification 이 조회한다.
 */
@Component
@RequiredArgsConstructor
public class NoticeNotificationAdapter implements NoticeNotificationPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void notifyCreated(Long noticeId, List<Long> memberIds) {
        applicationEventPublisher.publishEvent(new PushRequestedEvent(memberIds, NotificationType.NOTICE, noticeId));
        applicationEventPublisher.publishEvent(new CreateAlertMembersEvent(noticeId, memberIds, AlertType.NOTICE));
    }
}
