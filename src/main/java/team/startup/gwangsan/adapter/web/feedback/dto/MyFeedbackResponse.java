package team.startup.gwangsan.adapter.web.feedback.dto;

import java.time.LocalDateTime;
import java.util.List;
import team.startup.gwangsan.application.feedback.data.FeedbackDetail;
import team.startup.gwangsan.domain.feedback.model.*;

public record MyFeedbackResponse(Long feedbackId, FeedbackType type, String content,
                                 List<FeedbackDetail.FeedbackImage> images, FeedbackStatus status,
                                 String answer, LocalDateTime answeredAt, LocalDateTime createdAt) {
    public static MyFeedbackResponse from(FeedbackDetail detail) {
        Feedback f = detail.feedback();
        return new MyFeedbackResponse(f.id(), f.type(), f.content(), detail.images(), f.status(), f.answer(),
                f.answeredAt(), f.createdAt());
    }
}
