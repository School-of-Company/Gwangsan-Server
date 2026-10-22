package team.startup.gwangsan.adapter.web.admin.dto.response;

import team.startup.gwangsan.adapter.web.image.dto.GetImageResponse;
import team.startup.gwangsan.application.feedback.data.FeedbackDetail;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.time.LocalDateTime;
import java.util.List;

public record AdminFeedbackResponse(
        Long feedbackId,
        Long memberId,
        String nickname,
        String placeName,
        FeedbackType type,
        String content,
        List<GetImageResponse> images,
        String appVersion,
        OsType osType,
        String osVersion,
        String deviceModel,
        FeedbackStatus status,
        String answer,
        LocalDateTime answeredAt,
        String answeredBy,
        LocalDateTime createdAt
) {
    public static AdminFeedbackResponse from(FeedbackDetail detail) {
        return new AdminFeedbackResponse(detail.feedbackId(), detail.memberId(), detail.nickname(), detail.placeName(),
                detail.type(), detail.content(),
                detail.images().stream().map(i -> new GetImageResponse(i.imageId(), i.imageUrl())).toList(),
                detail.appVersion(), detail.osType(), detail.osVersion(), detail.deviceModel(), detail.status(),
                detail.answer(), detail.answeredAt(), detail.answeredByNickname(), detail.createdAt());
    }
}
