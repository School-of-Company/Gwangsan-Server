package team.startup.gwangsan.application.feedback.port.in;

public interface FeedbackCleanupUseCase {
    void reassign(Long memberId, Long dummyMemberId);
    void unlink(Long feedbackId, Long imageId);
}
