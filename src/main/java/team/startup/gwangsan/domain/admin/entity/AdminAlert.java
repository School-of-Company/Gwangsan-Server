package team.startup.gwangsan.domain.admin.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_admin_alert",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"type", "source_id"})})
@EntityListeners(AuditingEntityListener.class)
public class AdminAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_alert_id")
    private Long id;

    @Column(name = "type", nullable = false)
    private AlertType type;

    @Column(name = "title", nullable = false)
    private String title;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Builder
    public AdminAlert(AlertType type, String title, Long sourceId, Member member) {
        this.type = type;
        this.title = title;
        this.sourceId = sourceId;
        this.member = member;
    }
}
