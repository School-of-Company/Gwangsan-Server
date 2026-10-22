package team.startup.gwangsan.application.feedback.port.out;

import team.startup.gwangsan.domain.feedback.model.Feedback;

import java.util.List;

public interface SaveFeedbackPort {

    Long save(Feedback feedback);

    void saveImages(Long feedbackId, List<Long> imageIds);

    void deleteImageLink(Long feedbackId, Long imageId);

    /** answer/status/answeredBy/answeredAt 을 반영한다. */
    void saveAnswer(Feedback feedback);

    void changeStatus(Feedback feedback);

    /** 작성자·답변자 참조를 더미 회원으로 재지정한다. */
    void reassignMember(Long memberId, Long dummyMemberId);
}
