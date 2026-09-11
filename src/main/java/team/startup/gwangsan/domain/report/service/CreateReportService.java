package team.startup.gwangsan.domain.report.service;

import team.startup.gwangsan.domain.report.presentation.dto.request.CreateReportRequest;

public interface CreateReportService {
    void execute(CreateReportRequest request);
}
