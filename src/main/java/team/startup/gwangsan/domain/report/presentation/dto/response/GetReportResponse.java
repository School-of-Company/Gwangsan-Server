package team.startup.gwangsan.domain.report.presentation.dto.response;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.report.entity.constant.ReportType;

import java.util.List;

public record GetReportResponse(
        Long reportId,
        ReportType reportType,
        String content,
        List<GetImageResponse> images
) {
}
