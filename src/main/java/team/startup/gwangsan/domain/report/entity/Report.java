package team.startup.gwangsan.domain.report.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.domain.member.entity.Member;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_report")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "title", nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_id")
    private Member reported;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private Member reporter;

    @Builder
    public Report(String content, String title, Member reported, Member reporter) {
        this.content = content;
        this.title = title;
        this.reported = reported;
        this.reporter = reporter;
    }
}
