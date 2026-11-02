package team.startup.gwangsan.adapter.web.feedback.dto;

import java.time.LocalDateTime;
import java.util.List;
import team.startup.gwangsan.application.feedback.data.FeedbackDetail;
import team.startup.gwangsan.domain.feedback.model.*;

public record AdminFeedbackResponse(Long feedbackId, Long memberId, String nickname, String placeName,
                                    FeedbackType type, String content, List<FeedbackDetail.FeedbackImage> images,
                                    String appVersion, OsType osType, String osVersion, String deviceModel,
                                    FeedbackStatus status, String answer, LocalDateTime answeredAt,
                                    String answeredBy, LocalDateTime createdAt) {
    public static AdminFeedbackResponse from(FeedbackDetail detail) {
        Feedback f = detail.feedback();
        return new AdminFeedbackResponse(f.id(), f.memberId(), detail.nickname(), detail.placeName(), f.type(),
                f.content(), detail.images(), f.appVersion(), f.osType(), f.osVersion(), f.deviceModel(),
                f.status(), f.answer(), f.answeredAt(), detail.answeredBy(), f.createdAt());
    }
}
