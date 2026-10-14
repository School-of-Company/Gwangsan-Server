package team.startup.gwangsan.adapter.persistence.alert;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_alert_receipt",
        uniqueConstraints = @UniqueConstraint(columnNames = {"alert_id", "member_id"}))
@Getter
@NoArgsConstructor
public class AlertReceiptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_receipt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private AlertJpaEntity alert;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberJpaEntity member;

    @Column(name = "checked", nullable = false)
    private boolean checked;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Builder
    public AlertReceiptJpaEntity(AlertJpaEntity alert, MemberJpaEntity member, boolean checked) {
        this.alert = alert;
        this.member = member;
        this.checked = checked;
    }

    public void markChecked(LocalDateTime now) {
        if (!this.checked) {
            this.checked = true;
            this.checkedAt = now;
        }
    }
}
