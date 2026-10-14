package team.startup.gwangsan.domain.admin.presentation.dto.response;

import team.startup.gwangsan.domain.report.model.ReportTargetType;
import team.startup.gwangsan.adapter.web.report.dto.GetReportResponse;

import java.time.LocalDateTime;

public record GetReportAlertResponse(
        Long id,
        String nickname,
        Long reportedMemberId,
        String reportedMemberName,
        ReportTargetType targetType,
        Long productId,
        String productTitle,
        String title,
        String placeName,
        LocalDateTime createdAt,
        GetReportResponse report
) {
}
