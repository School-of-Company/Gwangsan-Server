package team.startup.gwangsan.application.report.port.out;

import team.startup.gwangsan.domain.report.model.Report;

import java.util.List;

public interface SaveReportPort {

    /** @return 저장된 신고 ID */
    Long save(Report report);

    void saveImages(Long reportId, List<Long> imageIds);

    void deleteImageLink(Long reportId, Long imageId);
}
