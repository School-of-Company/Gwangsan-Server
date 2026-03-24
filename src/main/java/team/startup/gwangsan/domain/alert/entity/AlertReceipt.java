package team.startup.gwangsan.domain.alert.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.domain.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_alert_receipt",
        uniqueConstraints = @UniqueConstraint(columnNames = {"alert_id", "member_id"}))
@Getter
@NoArgsConstructor
public class AlertReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_receipt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "checked", nullable = false)
    private boolean checked;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Builder
    public AlertReceipt(Alert alert, Member member, boolean checked) {
        this.alert = alert;
        this.member = member;
        this.checked = checked;
    }

    public void markChecked() {
        if (!this.checked) {
            this.checked = true;
            this.checkedAt = LocalDateTime.now();
        }
    }
}