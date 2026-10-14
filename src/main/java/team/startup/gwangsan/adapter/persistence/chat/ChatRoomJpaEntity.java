package team.startup.gwangsan.adapter.persistence.chat;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;

import java.time.LocalDateTime;

/**
 * tbl_chat_room 저장 모델. MemberJpaEntity/Product association 은 기존 FK·스키마 보존을 위한 저장 기술 예외다.
 * 업무 규칙은 domain.chat.model.ChatRoom 에 있다.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_chat_room")
@EntityListeners(AuditingEntityListener.class)
public class ChatRoomJpaEntity {

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
    public ChatRoomJpaEntity(LocalDateTime createdAt, Boolean isActive, MemberJpaEntity buyer, MemberJpaEntity seller, Product product) {
        this.createdAt = createdAt;
        this.isActive = isActive;
        this.buyer = buyer;
        this.seller = seller;
        this.product = product;
    }

    void updateHiddenState(LocalDateTime hiddenByBuyerAt, LocalDateTime hiddenBySellerAt) {
        this.hiddenByBuyerAt = hiddenByBuyerAt;
        this.hiddenBySellerAt = hiddenBySellerAt;
    }
}
