package team.startup.gwangsan.relatedkeyword.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.member.entity.Member;

import java.util.List;

@Entity
@Getter
@Table(name = "tbl_related_keyword")
@NoArgsConstructor
public class RelatedKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_related_keyword_id", nullable = false)
    private List<MemberRelatedKeyword> memberRelatedKeyword;

    @Builder
    public RelatedKeyword(String name, List<MemberRelatedKeyword> memberRelatedKeyword) {
        this.name = name;
        this.memberRelatedKeyword = memberRelatedKeyword;
    }
}
