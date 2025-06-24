package team.startup.gwangsan.domain.relatedkeyword.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Builder
    public RelatedKeyword(String name) {
        this.name = name;
    }
}
