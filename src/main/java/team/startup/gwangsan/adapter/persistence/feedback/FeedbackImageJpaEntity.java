package team.startup.gwangsan.adapter.persistence.feedback;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_feedback_image", uniqueConstraints = @UniqueConstraint(columnNames = {"feedback_id", "image_id"}))
public class FeedbackImageJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_image_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private FeedbackJpaEntity feedback;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private ImageJpaEntity image;

    public FeedbackImageJpaEntity(FeedbackJpaEntity feedback, ImageJpaEntity image) {
        this.feedback = feedback;
        this.image = image;
    }
}
