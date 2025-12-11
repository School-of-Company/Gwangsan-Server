package team.startup.gwangsan.domain.trade.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_trade_complete",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"product_id", "buyer_id", "seller_id", "status"})})
@EntityListeners(AuditingEntityListener.class)
public class TradeComplete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", referencedColumnName = "member_id", nullable = false)
    private Member buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", referencedColumnName = "member_id", nullable = false)
    private Member seller;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TradeStatus status;

    @Builder
    public TradeComplete(Product product, Member buyer, Member seller, TradeStatus status) {
        this.product = product;
        this.buyer = buyer;
        this.seller = seller;
        this.status = status;
    }

    public void updateStatus(TradeStatus status) {
        this.status = status;
    }

    public void updateCompletedAt() {
        this.completedAt = LocalDateTime.now();
    }
}