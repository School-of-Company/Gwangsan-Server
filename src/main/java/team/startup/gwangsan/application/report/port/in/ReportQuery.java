package team.startup.gwangsan.application.report.port.in;

import team.startup.gwangsan.application.report.data.ReportAlertSource;
import team.startup.gwangsan.application.report.data.ReportDetail;
import team.startup.gwangsan.application.report.data.ReportImageLink;
import team.startup.gwangsan.domain.report.model.ReportType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 다른 업무(alert·admin 등)가 쓰는 신고 공개 조회. */
public interface ReportQuery {

    /** @throws team.startup.gwangsan.domain.report.exception.NotFoundReportException 신고가 없으면 */
    ReportAlertSource getAlertSource(Long reportId);

    /** 신고 ID 별 이미지 연결. 이미지가 없는 신고는 결과에 없다. 빈 입력이면 조회하지 않는다. */
    Map<Long, List<ReportImageLink>> findImageLinks(Collection<Long> reportIds);

    /** @throws team.startup.gwangsan.domain.report.exception.NotFoundReportException 신고가 없으면 */
    ReportType getReportType(Long reportId);

    /**
     * 신고 ID 별 상세(없는 ID 는 빠진다). 빈 입력이면 조회하지 않는다. 신고 1회(게시글 포함) + 신고가 있으면 이미지 1회를
     * 조회하고, 피신고자 닉네임은 기존처럼 지연 로딩한다(기존 관리자 조회와 같은 쿼리 수).
     */
    Map<Long, ReportDetail> findDetails(Collection<Long> reportIds);
}
