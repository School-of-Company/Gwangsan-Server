package team.startup.gwangsan.adapter.persistence.member;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberDetail;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;

@Entity(name = "MemberDetail")
@Table(name = "tbl_member_detail")
@NoArgsConstructor
@Getter
public class MemberDetailJpaEntity {

    @Id
    @Column(name = "member_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @MapsId
    @JoinColumn(name = "member_id", nullable = false)
    private MemberJpaEntity member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dong_id", nullable = false)
    private DongJpaEntity dong;

    /** 잔액은 원자적 UPDATE 로만 바꾼다. 낡은 엔티티 값이 다른 필드 변경과 함께 되쓰이지 않게 막는다. */
    @Column(name = "gwangsan", nullable = false, updatable = false)
    private Integer gwangsan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private PlaceJpaEntity place;

    @Column(name = "light", nullable = false)
    private Integer light;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Builder
    public MemberDetailJpaEntity(MemberJpaEntity member, DongJpaEntity dong, Integer gwangsan, PlaceJpaEntity place, Integer light, String description) {
        this.member = member;
        this.dong = dong;
        this.gwangsan = gwangsan;
        this.place = place;
        this.light = light;
        this.description = description;
    }

    /** 순수 MemberDetail 의 변경을 이 managed 엔티티에 반영한다. */
    void apply(MemberDetail detail) {
        this.description = detail.getDescription();
        this.light = detail.getLight();
    }

    // bridge: 아래 직접 변경 메서드는 legacy 호출자(post/trade/admin/review)용이다. 각 소유 PR(07/11)이 공개 command 로 교체한 뒤 지운다.
    public void updateDescription(String description) {
        this.description = description;
    }

    public void plusLight(Integer light) {
        this.light = Math.min(100, Math.max(0, (this.light != null ? this.light : 0) + (light != null ? light : 0)));
    }

    public void updatePlace(PlaceJpaEntity place) {
        this.place = place;
    }
}
