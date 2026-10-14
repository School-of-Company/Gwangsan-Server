package team.startup.gwangsan.adapter.persistence.notice;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;

import java.time.LocalDateTime;

// place/member 연관은 기존 스키마(FK) 보존용이다. 공지 core 는 id 만 사용한다.
@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_notice")
@EntityListeners(AuditingEntityListener.class)
public class NoticeJpaEntity {

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
    private PlaceJpaEntity place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberJpaEntity member;

    @Builder
    public NoticeJpaEntity(String title, String content, PlaceJpaEntity place, MemberJpaEntity member) {
        this.title = title;
        this.content = content;
        this.place = place;
        this.member = member;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
