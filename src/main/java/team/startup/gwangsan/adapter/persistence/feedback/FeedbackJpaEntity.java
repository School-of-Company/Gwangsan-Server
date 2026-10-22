package team.startup.gwangsan.adapter.persistence.feedback;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.feedback.model.*;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_feedback", indexes = {
        @Index(name = "idx_feedback_member_cursor", columnList = "member_id,feedback_id"),
        @Index(name = "idx_feedback_status_type_cursor", columnList = "status,type,feedback_id")
})
public class FeedbackJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberJpaEntity member;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private FeedbackType type;
    @Column(nullable = false, length = 1000)
    private String content;
    private String appVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private OsType osType;
    private String osVersion;
    private String deviceModel;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private FeedbackStatus status;
    @Column(columnDefinition = "TEXT")
    private String answer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    private MemberJpaEntity answeredBy;
    private LocalDateTime answeredAt;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public FeedbackJpaEntity(Feedback feedback, MemberJpaEntity member) {
        this.member = member;
        this.type = feedback.type();
        this.content = feedback.content();
        this.appVersion = feedback.appVersion();
        this.osType = feedback.osType();
        this.osVersion = feedback.osVersion();
        this.deviceModel = feedback.deviceModel();
        this.createdAt = feedback.createdAt();
        apply(feedback, null);
    }

    public void apply(Feedback feedback, MemberJpaEntity admin) {
        status = feedback.status();
        answer = feedback.answer();
        answeredBy = admin;
        answeredAt = feedback.answeredAt();
        updatedAt = feedback.updatedAt();
    }

    public Feedback toModel() {
        return new Feedback(id, member.getId(), type, content, appVersion, osType, osVersion, deviceModel,
                status, answer, answeredBy == null ? null : answeredBy.getId(), answeredAt, createdAt, updatedAt);
    }
}
