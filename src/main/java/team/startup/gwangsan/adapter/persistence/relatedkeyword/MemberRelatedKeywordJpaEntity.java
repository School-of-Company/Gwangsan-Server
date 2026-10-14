package team.startup.gwangsan.adapter.persistence.relatedkeyword;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_member_related_keyword")
public class MemberRelatedKeywordJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_related_keyword_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private RelatedKeywordJpaEntity relatedKeyword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberJpaEntity member;

    @Builder
    public MemberRelatedKeywordJpaEntity(RelatedKeywordJpaEntity relatedKeyword, MemberJpaEntity member) {
        this.relatedKeyword = relatedKeyword;
        this.member = member;
    }
}
