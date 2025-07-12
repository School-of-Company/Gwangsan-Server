package team.startup.gwangsan.domain.post.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Table(name = "tbl_product")
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "gwangsan", nullable = false)
    private Integer gwangsan;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(name = "mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private Mode mode;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Builder
    public Product(String title, String description, Integer gwangsan, Member member, ProductStatus status, Type type, Mode mode) {
        this.title = title;
        this.description = description;
        this.gwangsan = gwangsan;
        this.member = member;
        this.status = status;
        this.type = type;
        this.mode = mode;
    }

    public void update(Type type, Mode mode, String title, String description, Integer gwangsant) {
        this.title = title;
        this.description = description;
        this.gwangsan = gwangsant;
        this.status = ProductStatus.ONGOING;
        this.type = type;
        this.mode = mode;
    }

    public void updateStatus(ProductStatus status) {
        this.status = status;
    }
}
