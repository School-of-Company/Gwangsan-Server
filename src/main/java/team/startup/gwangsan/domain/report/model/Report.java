package team.startup.gwangsan.domain.report.model;

import team.startup.gwangsan.domain.report.exception.SelfReportNotAllowedException;

/**
 * 저장 전 신고. 게시글 신고는 productId 와 함께 게시글 작성자를 reportedId 로 담는다.
 * productId 존재 여부가 곧 신고 대상 종류다(회원 신고는 null).
 */
public record Report(
        Long reporterId,
        Long reportedId,
        Long productId,
        ReportType reportType,
        String content
) {

    public static Report create(Long reporterId, Long reportedId, Long productId, ReportType reportType, String content) {
        if (reporterId.equals(reportedId)) {
            throw new SelfReportNotAllowedException();
        }
        return new Report(reporterId, reportedId, productId, reportType, content);
    }

    public boolean isProductReport() {
        return productId != null;
    }
}
