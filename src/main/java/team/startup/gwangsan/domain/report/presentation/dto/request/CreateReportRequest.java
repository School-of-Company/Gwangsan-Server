package team.startup.gwangsan.domain.report.presentation.dto.request;

import team.startup.gwangsan.domain.report.entity.constant.ReportTargetType;
import team.startup.gwangsan.domain.report.entity.constant.ReportType;

import java.util.List;

public record CreateReportRequest(
        ReportTargetType targetType,
        Long sourceId,
        ReportType reportType,
        String content,
        List<Long> imageIds
) {
    /** 기존 클라이언트는 targetType 을 보내지 않는다. 그 요청은 종전처럼 회원 신고로 해석한다. */
    public ReportTargetType targetTypeOrMember() {
        return targetType == null ? ReportTargetType.MEMBER : targetType;
    }
}
