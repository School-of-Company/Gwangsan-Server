package team.startup.gwangsan.domain.report.presentation.dto.request;

import team.startup.gwangsan.domain.report.entity.constant.ReportType;

public record CreateProductReportRequest(
        Long productId,
        ReportType reportType,
        String content
) {}
