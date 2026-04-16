package team.startup.gwangsan.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.constant.ReportType;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByReporterAndReportedAndReportType(Member reporter, Member reported, ReportType reportType);

    List<Report> findAllByIdIn(List<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Report r SET r.reporter = :dummy WHERE r.reporter = :target")
    void reassignReporter(@Param("target") Member target, @Param("dummy") Member dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Report r SET r.reported = :dummy WHERE r.reported = :target")
    void reassignReported(@Param("target") Member target, @Param("dummy") Member dummy);
}
