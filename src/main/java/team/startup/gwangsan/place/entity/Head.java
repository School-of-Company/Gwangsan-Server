package team.startup.gwangsan.place.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_head")
public class Head {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "head_id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Builder
    public Head(String name) {
        this.name = name;
    }
}
