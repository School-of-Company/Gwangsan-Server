package team.startup.gwangsan.domain.report.service;

import team.startup.gwangsan.domain.report.presentation.dto.request.CreateMemberReportRequest;

public interface CreateMemberReportService {
    void execute(CreateMemberReportRequest request);
}
