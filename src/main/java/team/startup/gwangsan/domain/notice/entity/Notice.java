package team.startup.gwangsan.domain.notice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.place.entity.Place;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_notice")
@EntityListeners(AuditingEntityListener.class)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tbl_notice_target_roles", joinColumns = @JoinColumn(name = "notice_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private List<MemberRole> targetRoles = new ArrayList<>();


    @Builder
    public Notice(String title, String content, Place place, Member member, List<MemberRole> targetRoles) {
        this.title = title;
        this.content = content;
        this.place = place;
        this.member = member;
        this.targetRoles = targetRoles;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
