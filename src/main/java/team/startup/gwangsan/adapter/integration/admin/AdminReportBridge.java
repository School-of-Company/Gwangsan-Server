package team.startup.gwangsan.adapter.integration.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.report.ReportJpaRepository;
import team.startup.gwangsan.application.admin.port.out.AdminReportPort;
import team.startup.gwangsan.application.report.port.in.ReportQuery;
import team.startup.gwangsan.domain.admin.policy.AdminAlertTitle;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;

/**
 * 신고자 ID 는 report(PR09) 공개 query 를 쓴다.
 * legacy bridge: 관리자 알림 제목에 필요한 신고 종류는 공개 query 에 없어 report 저장 모델을 읽는다. 제거: 신고 종류 공개 query → PR12.
 */
@Component
@RequiredArgsConstructor
public class AdminReportBridge implements AdminReportPort {

    private final ReportQuery reportQuery;
    private final ReportJpaRepository reportRepository;

    @Override
    public AdminAlertTitle.ReportCategory categoryOf(Long reportId) {
        return AdminAlertTitle.ReportCategory.valueOf(reportRepository.findById(reportId)
                .orElseThrow(NotFoundReportException::new)
                .getReportType().name());
    }

    @Override
    public Long reporterIdOf(Long reportId) {
        return reportQuery.getAlertSource(reportId).reporterId();
    }
}
