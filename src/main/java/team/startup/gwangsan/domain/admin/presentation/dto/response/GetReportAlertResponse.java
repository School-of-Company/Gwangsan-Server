package team.startup.gwangsan.domain.admin.presentation.dto.response;

import team.startup.gwangsan.domain.report.presentation.dto.response.GetReportResponse;

import java.time.LocalDateTime;

public record GetReportAlertResponse(
        Long reportId,
        String nickname,
        Long reportedMemberId,
        String reportedMemberName,
        String title,
        LocalDateTime createdAt,
        GetReportResponse report
) {
}
