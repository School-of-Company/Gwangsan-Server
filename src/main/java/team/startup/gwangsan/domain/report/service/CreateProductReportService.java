package team.startup.gwangsan.domain.report.service;

import team.startup.gwangsan.domain.report.presentation.dto.request.CreateProductReportRequest;

public interface CreateProductReportService {
    void execute(CreateProductReportRequest request);
}
