package team.startup.gwangsan.adapter.persistence.notice;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_notice_image")
public class NoticeImageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private ImageJpaEntity image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private NoticeJpaEntity notice;

    @Builder
    public NoticeImageJpaEntity(ImageJpaEntity image, NoticeJpaEntity notice) {
        this.image = image;
        this.notice = notice;
    }
}
