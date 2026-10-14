package team.startup.gwangsan.adapter.persistence.trade;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_trade_cancel")
@EntityListeners(AuditingEntityListener.class)
public class TradeCancelJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_cancel_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private TradeCompleteJpaEntity tradeComplete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberJpaEntity member;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeCancelStatus status;


    @Builder
    public TradeCancelJpaEntity(TradeCompleteJpaEntity tradeComplete, MemberJpaEntity member, String reason, TradeCancelStatus status) {
        this.tradeComplete = tradeComplete;
        this.member = member;
        this.reason = reason;
        this.status = status;
    }

    public void updateStatus(TradeCancelStatus status) {
        this.status = status;
    }
}
