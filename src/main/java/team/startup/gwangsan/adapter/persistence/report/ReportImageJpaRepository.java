package team.startup.gwangsan.adapter.persistence.report;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.adapter.persistence.report.custom.ReportImageCustomRepository;

public interface ReportImageJpaRepository extends JpaRepository<ReportImageJpaEntity, Long>, ReportImageCustomRepository {
    void deleteByReportIdAndImageId(Long reportId, Long imageId);
}
