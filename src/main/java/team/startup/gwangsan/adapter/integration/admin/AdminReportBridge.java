package team.startup.gwangsan.adapter.integration.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.admin.port.out.AdminReportPort;
import team.startup.gwangsan.application.report.port.in.ReportQuery;
import team.startup.gwangsan.domain.admin.policy.AdminAlertTitle;

/** report(PR09) 공개 query 연결. 없는 신고는 기존 NotFoundReport. */
@Component
@RequiredArgsConstructor
public class AdminReportBridge implements AdminReportPort {

    private final ReportQuery reportQuery;

    @Override
    public AdminAlertTitle.ReportCategory categoryOf(Long reportId) {
        return AdminAlertTitle.ReportCategory.valueOf(reportQuery.getReportType(reportId).name());
    }

    @Override
    public Long reporterIdOf(Long reportId) {
        return reportQuery.getAlertSource(reportId).reporterId();
    }
}
