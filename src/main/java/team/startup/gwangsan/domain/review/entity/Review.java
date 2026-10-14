package team.startup.gwangsan.domain.review.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "light", nullable = false)
    private Integer light;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private MemberJpaEntity reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_id", nullable = false)
    private MemberJpaEntity reviewed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Builder
    public Review(String content, Integer light, MemberJpaEntity reviewer, MemberJpaEntity reviewed, Product product) {
        this.content = content;
        this.light = light;
        this.reviewer = reviewer;
        this.reviewed = reviewed;
        this.product = product;
    }
}
