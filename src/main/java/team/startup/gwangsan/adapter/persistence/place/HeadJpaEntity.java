package team.startup.gwangsan.adapter.persistence.place;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_head_place")
public class HeadJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "head_id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Builder
    public HeadJpaEntity(String name) {
        this.name = name;
    }
}
