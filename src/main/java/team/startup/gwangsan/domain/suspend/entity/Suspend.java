package team.startup.gwangsan.domain.suspend.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.domain.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_suspend")
@EntityListeners(AuditingEntityListener.class)
public class Suspend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suspend_id")
    private Long id;

    @Column(name = "suspended_at", nullable = false)
    private LocalDateTime suspendedAt;

    @Column(name = "suspended_days", nullable = false)
    private Integer suspendedDays;

    @Column(name = "suspended_until", nullable = false)
    private LocalDateTime suspendedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Builder
    public Suspend(LocalDateTime suspendedAt, Integer suspendedDays, LocalDateTime suspendedUntil, Member member) {
        this.suspendedAt = suspendedAt;
        this.suspendedDays = suspendedDays;
        this.suspendedUntil = suspendedUntil;
        this.member = member;
    }
}
