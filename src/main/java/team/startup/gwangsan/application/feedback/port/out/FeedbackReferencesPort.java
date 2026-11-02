package team.startup.gwangsan.application.feedback.port.out;

import java.util.List;

public interface FeedbackReferencesPort {
    void requireMember(Long memberId);
    Integer requirePlace(Long memberId);
    void requireImages(List<Long> imageIds);
    void notifyAnswered(Long feedbackId, Long memberId, Long adminId);
}
