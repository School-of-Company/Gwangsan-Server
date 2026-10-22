package team.startup.gwangsan.adapter.persistence.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackImageJpaRepository extends JpaRepository<FeedbackImageJpaEntity, Long>, FeedbackImageCustomRepository {
    void deleteByFeedbackIdAndImageId(Long feedbackId, Long imageId);
}
