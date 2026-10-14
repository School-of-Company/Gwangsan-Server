package team.startup.gwangsan.application.report.port.out;

import team.startup.gwangsan.application.report.data.ReportAlertSource;
import team.startup.gwangsan.application.report.data.ReportImageLink;
import team.startup.gwangsan.domain.report.model.ReportType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadReportPort {

    /** 회원 신고 중복. 게시글 신고 행(product 존재)은 제외한다. */
    boolean existsMemberReport(Long reporterId, Long reportedId, ReportType reportType);

    /** 게시글 신고 중복. 같은 작성자의 다른 게시글은 별개 신고다. */
    boolean existsProductReport(Long reporterId, Long productId, ReportType reportType);

    Optional<ReportAlertSource> findAlertSource(Long reportId);

    Map<Long, List<ReportImageLink>> findImageLinks(Collection<Long> reportIds);
}
