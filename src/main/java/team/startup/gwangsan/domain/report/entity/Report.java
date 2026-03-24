package team.startup.gwangsan.domain.report.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.report.entity.constant.ReportType;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_report")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "report_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Column(name = "content", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_id")
    private Member reported;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private Member reporter;

    @Builder
    public Report(ReportType reportType, String content, Member reported, Member reporter) {
        this.reportType = reportType;
        this.content = content;
        this.reported = reported;
        this.reporter = reporter;
    }
}
