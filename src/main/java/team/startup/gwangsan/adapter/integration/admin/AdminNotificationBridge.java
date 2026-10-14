package team.startup.gwangsan.adapter.integration.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.admin.data.MemberAlertKind;
import team.startup.gwangsan.application.admin.port.out.AdminNotificationPort;
import team.startup.gwangsan.adapter.event.alert.CreateAlertEvent;
import team.startup.gwangsan.domain.alert.model.AlertType;

/**
 * legacy bridge: alert(PR04) 미전환. 기존 CreateAlertEvent(AFTER_COMMIT·비동기)를 그대로 발행한다.
 * 관리자 처리 알림(REPORT_REJECT·TRADE_CANCEL_REJECT·RECOMMENDER)은 확정된 원자 저장 범위 밖이라 비동기를 유지한다.
 * 제거: alert 공개 생성 Port 로 교체(PR04 → PR12).
 */
@Component
@RequiredArgsConstructor
public class AdminNotificationBridge implements AdminNotificationPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void notifyMember(Long sourceId, Long memberId, MemberAlertKind kind) {
        applicationEventPublisher.publishEvent(new CreateAlertEvent(sourceId, memberId, AlertType.valueOf(kind.name())));
    }
}
