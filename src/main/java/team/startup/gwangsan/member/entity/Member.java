package team.startup.gwangsan.member.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.member.entity.constant.MemberRole;
import team.startup.gwangsan.relatedkeyword.entity.MemberRelatedKeyword;

import java.util.List;

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

    @ManyToOne
    @JoinColumn(name = "recommender_id")
    private Member recommender;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_related_keyword", nullable = false)
    private List<MemberRelatedKeyword> memberRelatedKeyword;

    @Builder
    public Member(String name, String nickname, String phoneNumber, String password, Member recommender, MemberRole role, List<MemberRelatedKeyword> memberRelatedKeyword) {
        this.name = name;
        this.nickname = nickname;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.recommender = recommender;
        this.role = role;
        this.memberRelatedKeyword = memberRelatedKeyword;
    }
}
