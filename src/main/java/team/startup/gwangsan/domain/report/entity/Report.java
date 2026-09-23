package team.startup.gwangsan.domain.report.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
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

    /**
     * 게시글 신고일 때만 채워진다. 회원 신고는 null 이며, 이 값의 존재 여부가 곧 신고 대상 종류다.
     * 게시글 신고도 {@code reported} 에 게시글 작성자를 함께 담아, 관리자 정지·알림·탈퇴 재할당
     * 경로가 회원 신고와 동일하게 동작하도록 한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Builder
    public Report(ReportType reportType, String content, Member reported, Member reporter, Product product) {
        this.reportType = reportType;
        this.content = content;
        this.reported = reported;
        this.reporter = reporter;
        this.product = product;
    }
}
