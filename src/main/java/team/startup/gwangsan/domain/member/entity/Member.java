package team.startup.gwangsan.domain.member.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;

@Entity
@Table(name = "tbl_member")
@NoArgsConstructor
@Getter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "nickname", unique = true, nullable = false)
    private String nickname;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dong_id")
    private Dong dong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id")
    private RelatedKeyword specialty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommender_id")
    private Member recommender;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Column(name = "member_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @Builder
    public Member(String name, String nickname, String phoneNumber, String password, Member recommender, MemberRole role, MemberStatus status, Dong dong, Place place, RelatedKeyword specialty) {
        this.name = name;
        this.nickname = nickname;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.recommender = recommender;
        this.role = role;
        this.status = status;
        this.dong = dong;
        this.place = place;
        this.specialty = specialty;
    }
}
