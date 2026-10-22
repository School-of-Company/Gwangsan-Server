package team.startup.gwangsan.adapter.persistence.feedback;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_feedback")
@EntityListeners(AuditingEntityListener.class)
public class FeedbackJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberJpaEntity member;

    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FeedbackType type;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "app_version", length = 32)
    private String appVersion;

    @Column(name = "os_type", length = 10)
    @Enumerated(EnumType.STRING)
    private OsType osType;

    @Column(name = "os_version", length = 32)
    private String osVersion;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FeedbackStatus status;

    @Column(name = "answer", length = 1000)
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by_member_id")
    private MemberJpaEntity answeredBy;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public FeedbackJpaEntity(MemberJpaEntity member, FeedbackType type, String content, String appVersion,
                              OsType osType, String osVersion, String deviceModel, FeedbackStatus status) {
        this.member = member;
        this.type = type;
        this.content = content;
        this.appVersion = appVersion;
        this.osType = osType;
        this.osVersion = osVersion;
        this.deviceModel = deviceModel;
        this.status = status;
    }

    public void applyAnswer(String answer, MemberJpaEntity answeredBy, LocalDateTime answeredAt, FeedbackStatus status) {
        this.answer = answer;
        this.answeredBy = answeredBy;
        this.answeredAt = answeredAt;
        this.status = status;
    }

    public void updateStatus(FeedbackStatus status) {
        this.status = status;
    }
}
