package team.startup.gwangsan.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.ReportImage;

import java.util.Collection;
import java.util.List;

public interface ReportImageRepository extends JpaRepository<ReportImage, Long> {
    void deleteByReportIdAndImageId(Long reportId, Long imageId);

    List<ReportImage> findByReportIn(Collection<Report> reports);
}
