package team.startup.gwangsan.adapter.persistence.feedback;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_feedback_image")
public class FeedbackImageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private ImageJpaEntity image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private FeedbackJpaEntity feedback;

    @Builder
    public FeedbackImageJpaEntity(ImageJpaEntity image, FeedbackJpaEntity feedback) {
        this.image = image;
        this.feedback = feedback;
    }
}
