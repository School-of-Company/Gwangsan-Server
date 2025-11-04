package team.startup.gwangsan.domain.member.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.place.entity.Place;

@Entity
@Table(name = "tbl_member_detail")
@NoArgsConstructor
@Getter
@Check(constraints = "light >= 1 AND light <= 100")
public class MemberDetail {

    @Id
    @Column(name = "member_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @MapsId
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dong_id", nullable = false)
    private Dong dong;

    @Column(name = "gwangsan", nullable = false)
    private Integer gwangsan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "light", nullable = false)
    private Integer light;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Builder
    public MemberDetail(Member member, Dong dong, Integer gwangsan, Place place, Integer light, String description) {
        this.member = member;
        this.dong = dong;
        this.gwangsan = gwangsan;
        this.place = place;
        this.light = light;
        this.description = description;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void minusGwangsan(Integer gwangsan) {
        this.gwangsan = this.gwangsan - gwangsan;
    }

    public void plusGwangsan(Integer gwangsan) {
        this.gwangsan = this.gwangsan + gwangsan;
    }

    public void updatePlace(Place place) {
        this.place = place;
    }
}
