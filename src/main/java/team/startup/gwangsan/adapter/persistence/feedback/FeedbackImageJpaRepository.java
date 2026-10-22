package team.startup.gwangsan.adapter.persistence.feedback;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.*;

public interface FeedbackImageJpaRepository extends JpaRepository<FeedbackImageJpaEntity, Long> {
    @EntityGraph(attributePaths = "image")
    List<FeedbackImageJpaEntity> findAllByFeedbackIdInOrderByIdAsc(Collection<Long> feedbackIds);

    @Modifying
    @Query("delete from FeedbackImageJpaEntity f where f.feedback.id = :feedbackId and f.image.id = :imageId")
    void unlink(Long feedbackId, Long imageId);
}
