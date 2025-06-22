package team.startup.gwangsan.domain.relatedkeyword.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @OneToMany(mappedBy = "relatedKeyword", fetch = FetchType.LAZY)
    private List<MemberRelatedKeyword> memberRelatedKeyword;

    @Builder
    public RelatedKeyword(String name, List<MemberRelatedKeyword> memberRelatedKeyword) {
        this.name = name;
        this.memberRelatedKeyword = memberRelatedKeyword;
    }
}
