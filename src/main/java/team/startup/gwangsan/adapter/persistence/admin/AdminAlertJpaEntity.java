package team.startup.gwangsan.adapter.persistence.admin;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.time.LocalDateTime;

// 회원 연관은 기존 스키마(FK) 보존을 위한 저장 기술 예외다. 업무 계층에는 회원 ID 만 전달한다.
@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_admin_alert",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"alert_type", "source_id"})})
@EntityListeners(AuditingEntityListener.class)
public class AdminAlertJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_alert_id")
    private Long id;

    @Column(name = "alert_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertType type;

    @Column(name = "title", nullable = false)
    private String title;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "other_member_id", referencedColumnName = "member_id")
    private MemberJpaEntity otherMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", referencedColumnName = "member_id")
    private MemberJpaEntity requester;

    @Builder
    public AdminAlertJpaEntity(AlertType type, String title, Long sourceId, MemberJpaEntity otherMember, MemberJpaEntity requester) {
        this.type = type;
        this.title = title;
        this.sourceId = sourceId;
        this.otherMember = otherMember;
        this.requester = requester;
    }
}
