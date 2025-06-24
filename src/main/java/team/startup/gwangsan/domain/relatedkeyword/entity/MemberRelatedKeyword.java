package team.startup.gwangsan.domain.relatedkeyword.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.domain.member.entity.Member;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_member_related_keyword")
public class MemberRelatedKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_related_keyword_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private RelatedKeyword relatedKeyword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    public MemberRelatedKeyword(RelatedKeyword relatedKeyword, Member member) {
        this.relatedKeyword = relatedKeyword;
        this.member = member;
    }
}
