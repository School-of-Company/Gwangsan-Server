package team.startup.gwangsan.adapter.persistence.place;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_place")
public class PlaceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_id")
    private HeadJpaEntity head;

    @Builder
    public PlaceJpaEntity(String name, HeadJpaEntity head) {
        this.name = name;
        this.head = head;
    }
}
