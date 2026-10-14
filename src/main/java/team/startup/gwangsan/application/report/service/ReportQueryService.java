package team.startup.gwangsan.application.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.report.data.ReportAlertSource;
import team.startup.gwangsan.application.report.data.ReportDetail;
import team.startup.gwangsan.application.report.data.ReportImageLink;
import team.startup.gwangsan.application.report.port.in.ReassignReportMemberUseCase;
import team.startup.gwangsan.application.report.port.in.ReportQuery;
import team.startup.gwangsan.application.report.port.in.UnlinkReportImageUseCase;
import team.startup.gwangsan.application.report.port.out.LoadReportPort;
import team.startup.gwangsan.application.report.port.out.SaveReportPort;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.model.ReportType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportQueryService implements ReportQuery, UnlinkReportImageUseCase, ReassignReportMemberUseCase {

    private final LoadReportPort loadReportPort;
    private final SaveReportPort saveReportPort;

    @Override
    @Transactional(readOnly = true)
    public ReportAlertSource getAlertSource(Long reportId) {
        return loadReportPort.findAlertSource(reportId).orElseThrow(NotFoundReportException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<ReportImageLink>> findImageLinks(Collection<Long> reportIds) {
        if (reportIds.isEmpty()) {
            return Map.of();
        }
        return loadReportPort.findImageLinks(reportIds);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportType getReportType(Long reportId) {
        return loadReportPort.findReportType(reportId).orElseThrow(NotFoundReportException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ReportDetail> findDetails(Collection<Long> reportIds) {
        if (reportIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ReportDetail> details = new LinkedHashMap<>();
        loadReportPort.findDetails(reportIds).forEach(detail -> details.put(detail.reportId(), detail));
        return details;
    }

    // 회원 탈퇴 트랜잭션에 참여한다(REQUIRED). 별도 커밋하지 않는다.
    @Override
    @Transactional
    public void reassign(Long memberId, Long dummyMemberId) {
        saveReportPort.reassignMember(memberId, dummyMemberId);
    }

    @Override
    @Transactional
    public void unlink(Long reportId, Long imageId) {
        saveReportPort.deleteImageLink(reportId, imageId);
    }
}
