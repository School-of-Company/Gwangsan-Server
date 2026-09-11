package team.startup.gwangsan.domain.report.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.constant.ReportType;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 회원 신고의 중복 검사. {@code product} 가 없는 행만 본다. 이 조건을 빼면 같은 작성자의
     * 게시글을 이미 신고한 사용자가 그 회원 자체를 신고할 수 없게 잘못 막힌다.
     */
    Optional<Report> findByReporterAndReportedAndReportTypeAndProductIsNull(Member reporter, Member reported, ReportType reportType);

    /** 게시글 신고의 중복 검사. 같은 작성자의 다른 게시글은 별개 신고로 허용된다. */
    Optional<Report> findByReporterAndProductAndReportType(Member reporter, Product product, ReportType reportType);

    @EntityGraph(attributePaths = "product")
    List<Report> findAllByIdIn(List<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Report r SET r.reporter = :dummy WHERE r.reporter = :target")
    void reassignReporter(@Param("target") Member target, @Param("dummy") Member dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Report r SET r.reported = :dummy WHERE r.reported = :target")
    void reassignReported(@Param("target") Member target, @Param("dummy") Member dummy);
}
