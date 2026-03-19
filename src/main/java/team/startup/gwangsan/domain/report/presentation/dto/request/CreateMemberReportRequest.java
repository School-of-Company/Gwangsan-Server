package team.startup.gwangsan.domain.report.presentation.dto.request;

import team.startup.gwangsan.domain.report.entity.constant.ReportType;

import java.util.List;

public record CreateMemberReportRequest(
        Long sourceId,
        ReportType reportType,
        String content,
        List<Long> imageIds
) {}
