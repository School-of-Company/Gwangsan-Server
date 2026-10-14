package team.startup.gwangsan.domain.chat.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.post.entity.Product;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_chat_room")
@EntityListeners(AuditingEntityListener.class)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private MemberJpaEntity buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private MemberJpaEntity seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "hidden_by_buyer_at")
    private LocalDateTime hiddenByBuyerAt;

    @Column(name = "hidden_by_seller_at")
    private LocalDateTime hiddenBySellerAt;

    @Builder
    public ChatRoom(LocalDateTime createdAt, Boolean isActive, MemberJpaEntity buyer, MemberJpaEntity seller, Product product) {
        this.createdAt = createdAt;
        this.isActive = isActive;
        this.buyer = buyer;
        this.seller = seller;
        this.product = product;
    }

    public boolean isParticipant(MemberJpaEntity member) {
        return buyer.getId().equals(member.getId()) || seller.getId().equals(member.getId());
    }

    public MemberJpaEntity getOtherMember(MemberJpaEntity member) {
        return buyer.getId().equals(member.getId()) ? seller : buyer;
    }

    public boolean isHiddenFor(MemberJpaEntity member) {
        if (buyer.getId().equals(member.getId())) {
            return hiddenByBuyerAt != null;
        }
        if (seller.getId().equals(member.getId())) {
            return hiddenBySellerAt != null;
        }
        return false;
    }

    public void hideFor(MemberJpaEntity member, LocalDateTime hiddenAt) {
        if (buyer.getId().equals(member.getId())) {
            this.hiddenByBuyerAt = hiddenAt;
        } else if (seller.getId().equals(member.getId())) {
            this.hiddenBySellerAt = hiddenAt;
        }
    }

    public void unhideFor(MemberJpaEntity member) {
        if (buyer.getId().equals(member.getId())) {
            this.hiddenByBuyerAt = null;
        } else if (seller.getId().equals(member.getId())) {
            this.hiddenBySellerAt = null;
        }
    }
}
