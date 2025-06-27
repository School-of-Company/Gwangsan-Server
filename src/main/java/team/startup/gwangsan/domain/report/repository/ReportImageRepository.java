package team.startup.gwangsan.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.report.entity.ReportImage;

public interface ReportImageRepository extends JpaRepository<ReportImage, Long> {
    void deleteByReportIdAndImageId(Long reportId, Long imageId);
}
