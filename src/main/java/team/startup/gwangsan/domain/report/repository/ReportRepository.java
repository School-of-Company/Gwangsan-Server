package team.startup.gwangsan.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.report.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
