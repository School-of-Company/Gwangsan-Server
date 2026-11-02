package team.startup.gwangsan.application.feedback.port.out;

import java.util.List;
import java.util.Optional;
import team.startup.gwangsan.domain.feedback.model.Feedback;
import team.startup.gwangsan.application.feedback.data.*;

public interface FeedbackPort {
    void create(Feedback feedback, List<Long> imageIds);
    Optional<FeedbackDetail> find(Long id);
    List<FeedbackDetail> search(FeedbackSearch search);
    void update(Feedback feedback);
    void reassign(Long memberId, Long dummyMemberId);
    void unlink(Long feedbackId, Long imageId);
}
