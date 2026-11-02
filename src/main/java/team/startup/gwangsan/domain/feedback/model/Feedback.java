package team.startup.gwangsan.domain.feedback.model;

import java.time.LocalDateTime;
import team.startup.gwangsan.domain.feedback.exception.InvalidFeedbackException;

public record Feedback(Long id, Long memberId, FeedbackType type, String content, String appVersion,
                       OsType osType, String osVersion, String deviceModel, FeedbackStatus status,
                       String answer, Long answeredBy, LocalDateTime answeredAt, LocalDateTime createdAt,
                       LocalDateTime updatedAt) {
    public Feedback answer(String value, Long adminId, LocalDateTime now) {
        if (value == null || value.isBlank()) throw new InvalidFeedbackException();
        return new Feedback(id, memberId, type, content, appVersion, osType, osVersion, deviceModel,
                FeedbackStatus.DONE, value, adminId, now, createdAt, now);
    }

    public Feedback changeStatus(FeedbackStatus value, LocalDateTime now) {
        if (value == null || value == FeedbackStatus.RECEIVED) throw new InvalidFeedbackException();
        return new Feedback(id, memberId, type, content, appVersion, osType, osVersion, deviceModel,
                value, answer, answeredBy, answeredAt, createdAt, now);
    }
}
