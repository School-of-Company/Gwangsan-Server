package team.startup.gwangsan.adapter.event.report;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.report.port.out.RequestReportAlertPort;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.application.admin.data.CreateAdminAlertEvent;

// 기존 CreateAdminAlertEvent(AFTER_COMMIT·@Async listener) 계약을 그대로 사용한다. 이벤트 소유 이동은 PR04/PR11.
@Component
@RequiredArgsConstructor
public class ReportAlertEventAdapter implements RequestReportAlertPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void requestAdminAlert(Long reportId, Long reporterId) {
        eventPublisher.publishEvent(new CreateAdminAlertEvent(AlertType.REPORT, reportId, reporterId));
    }
}
