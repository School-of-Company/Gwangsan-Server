package team.startup.gwangsan.adapter.persistence.alert;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.domain.alert.model.AlertType;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.time.LocalDateTime;

// MemberJpaEntity 연관은 기존 FK(send_member_id) 보존을 위한 저장 기술 예외다. 업무 core 에는 회원 ID 만 노출한다.
@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_alert")
@EntityListeners(AuditingEntityListener.class)
public class AlertJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long id;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "send_member_id")
    private MemberJpaEntity sendMember;

    @Column(name = "alert_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AlertJpaEntity(Long sourceId, MemberJpaEntity sendMember, AlertType alertType, String title, String content) {
        this.sourceId = sourceId;
        this.sendMember = sendMember;
        this.alertType = alertType;
        this.title = title;
        this.content = content;
    }
}
