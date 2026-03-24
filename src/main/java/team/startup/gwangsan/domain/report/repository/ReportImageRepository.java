package team.startup.gwangsan.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.report.entity.ReportImage;
import team.startup.gwangsan.domain.report.repository.custom.ReportImageCustomRepository;

public interface ReportImageRepository extends JpaRepository<ReportImage, Long>, ReportImageCustomRepository {
    void deleteByReportIdAndImageId(Long reportId, Long imageId);
}
