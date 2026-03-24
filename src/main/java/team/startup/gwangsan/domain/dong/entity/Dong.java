package team.startup.gwangsan.domain.dong.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tbl_dong")
@NoArgsConstructor
public class Dong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dong_id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Builder
    public Dong(String name) {
        this.name = name;
    }
}
