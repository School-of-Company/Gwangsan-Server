package team.startup.gwangsan.application.admin.data;

import java.time.LocalDateTime;
import java.util.List;

public record AdminAlertsResult(
        List<ReportAlert> reports,
        List<SignUpAlert> signUps,
        List<TradeCancelAlert> tradeCancels
) {
    /** reportType 은 report 의 ReportType 이름. productTarget 이 false 면 회원 신고. */
    public record ReportAlert(
            Long id,
            String nickname,
            Long reportedMemberId,
            String reportedMemberName,
            boolean productTarget,
            Long productId,
            String productTitle,
            String title,
            String placeName,
            LocalDateTime createdAt,
            Long reportId,
            String reportType,
            String content,
            List<AdminImage> images
    ) {
    }

    public record SignUpAlert(
            Long id,
            Long memberId,
            String nickname,
            String title,
            String placeName,
            String recommenderNickname,
            LocalDateTime createdAt
    ) {
    }

    public record TradeCancelAlert(
            Long id,
            String nickname,
            String title,
            String reason,
            String placeName,
            LocalDateTime createdAt,
            List<AdminImage> images,
            AdminProduct product
    ) {
    }
}
