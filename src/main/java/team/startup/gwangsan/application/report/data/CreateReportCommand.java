package team.startup.gwangsan.application.report.data;

import team.startup.gwangsan.domain.report.model.ReportTargetType;
import team.startup.gwangsan.domain.report.model.ReportType;

import java.util.List;

/** targetType 은 web adapter 에서 기본값(MEMBER)을 적용한 값이다. */
public record CreateReportCommand(
        Long reporterId,
        ReportTargetType targetType,
        Long sourceId,
        ReportType reportType,
        String content,
        List<Long> imageIds
) {
}
