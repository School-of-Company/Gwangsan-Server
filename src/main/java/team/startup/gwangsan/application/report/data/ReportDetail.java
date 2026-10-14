package team.startup.gwangsan.application.report.data;

import team.startup.gwangsan.domain.report.model.ReportType;

import java.util.List;

/**
 * 관리자 신고 알림 목록용 신고 상세. 피신고자·게시글은 없으면 null 이다.
 * images 의 imageId 는 이미지 ID 다(신고-이미지 연결 행 ID 가 아님, 기존 관리자 응답 계약).
 */
public record ReportDetail(
        Long reportId,
        ReportType reportType,
        String content,
        Long reportedId,
        String reportedNickname,
        Long productId,
        String productTitle,
        List<ReportImageRef> images
) {
    public record ReportImageRef(Long imageId, String imageUrl) {
    }
}
