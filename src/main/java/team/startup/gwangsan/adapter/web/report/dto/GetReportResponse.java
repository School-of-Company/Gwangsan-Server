package team.startup.gwangsan.adapter.web.report.dto;

import team.startup.gwangsan.adapter.web.image.dto.GetImageResponse;
import team.startup.gwangsan.domain.report.model.ReportType;

import java.util.List;

public record GetReportResponse(
        Long reportId,
        ReportType reportType,
        String content,
        List<GetImageResponse> images
) {
}
