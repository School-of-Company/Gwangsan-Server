package team.startup.gwangsan.adapter.web.feedback.dto.response;

import team.startup.gwangsan.adapter.web.image.dto.GetImageResponse;
import team.startup.gwangsan.application.feedback.data.FeedbackSummary;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;

import java.time.LocalDateTime;
import java.util.List;

public record FeedbackSummaryResponse(
        Long feedbackId,
        FeedbackType type,
        String content,
        List<GetImageResponse> images,
        FeedbackStatus status,
        String answer,
        LocalDateTime answeredAt,
        LocalDateTime createdAt
) {
    public static FeedbackSummaryResponse from(FeedbackSummary summary) {
        return new FeedbackSummaryResponse(summary.feedbackId(), summary.type(), summary.content(),
                summary.images().stream().map(i -> new GetImageResponse(i.imageId(), i.imageUrl())).toList(),
                summary.status(), summary.answer(), summary.answeredAt(), summary.createdAt());
    }
}
