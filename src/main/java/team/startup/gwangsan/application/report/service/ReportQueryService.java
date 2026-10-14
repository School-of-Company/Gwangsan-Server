package team.startup.gwangsan.application.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.report.data.ReportAlertSource;
import team.startup.gwangsan.application.report.data.ReportImageLink;
import team.startup.gwangsan.application.report.port.in.ReportQuery;
import team.startup.gwangsan.application.report.port.in.UnlinkReportImageUseCase;
import team.startup.gwangsan.application.report.port.out.LoadReportPort;
import team.startup.gwangsan.application.report.port.out.SaveReportPort;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportQueryService implements ReportQuery, UnlinkReportImageUseCase {

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
    @Transactional
    public void unlink(Long reportId, Long imageId) {
        saveReportPort.deleteImageLink(reportId, imageId);
    }
}
