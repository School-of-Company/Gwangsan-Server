package team.startup.gwangsan.application.report.port.in;

import team.startup.gwangsan.application.report.data.ReportAlertSource;
import team.startup.gwangsan.application.report.data.ReportImageLink;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 다른 업무(alert 등)가 쓰는 신고 공개 조회. */
public interface ReportQuery {

    /** @throws team.startup.gwangsan.domain.report.exception.NotFoundReportException 신고가 없으면 */
    ReportAlertSource getAlertSource(Long reportId);

    /** 신고 ID 별 이미지 연결. 이미지가 없는 신고는 결과에 없다. 빈 입력이면 조회하지 않는다. */
    Map<Long, List<ReportImageLink>> findImageLinks(Collection<Long> reportIds);
}
