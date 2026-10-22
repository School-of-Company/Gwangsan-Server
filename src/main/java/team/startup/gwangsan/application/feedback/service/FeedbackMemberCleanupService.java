package team.startup.gwangsan.application.feedback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.feedback.port.in.ReassignFeedbackMemberUseCase;
import team.startup.gwangsan.application.feedback.port.in.UnlinkFeedbackImageUseCase;
import team.startup.gwangsan.application.feedback.port.out.SaveFeedbackPort;

@Service
@RequiredArgsConstructor
public class FeedbackMemberCleanupService implements ReassignFeedbackMemberUseCase, UnlinkFeedbackImageUseCase {

    private final SaveFeedbackPort saveFeedbackPort;

    @Override
    @Transactional
    public void reassign(Long memberId, Long dummyMemberId) {
        saveFeedbackPort.reassignMember(memberId, dummyMemberId);
    }

    @Override
    @Transactional
    public void unlink(Long feedbackId, Long imageId) {
        saveFeedbackPort.deleteImageLink(feedbackId, imageId);
    }
}
