package team.startup.gwangsan.application.feedback.data;

import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;

import java.time.LocalDateTime;
import java.util.List;

/** 회원 본인의 문의 내역 한 건. */
public record FeedbackSummary(
        Long feedbackId,
        FeedbackType type,
        String content,
        List<FeedbackImageLink> images,
        FeedbackStatus status,
        String answer,
        LocalDateTime answeredAt,
        LocalDateTime createdAt
) {
}
