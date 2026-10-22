package team.startup.gwangsan.adapter.persistence.feedback;

import java.util.Collection;
import java.util.List;

public interface FeedbackImageCustomRepository {
    List<FeedbackImageJpaEntity> findAllByFeedbackIdIn(Collection<Long> feedbackIds);
}
