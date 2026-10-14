package team.startup.gwangsan.adapter.event.suspend;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.suspend.port.out.RequestSuspensionAlertPort;
import team.startup.gwangsan.domain.alert.model.AlertType;
import team.startup.gwangsan.adapter.event.alert.CreateAlertEvent;

// 확정 정책: 정지 REPORT 알림은 원자 저장 대상이 아니므로 기존 CreateAlertEvent(AFTER_COMMIT·@Async, PR04 AlertEventListener)를 유지한다.
@Component
@RequiredArgsConstructor
public class SuspensionAlertEventAdapter implements RequestSuspensionAlertPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void requestReportAlert(Long reportId, Long memberId, Long suspendId) {
        eventPublisher.publishEvent(new CreateAlertEvent(reportId, memberId, AlertType.REPORT, suspendId));
    }
}
