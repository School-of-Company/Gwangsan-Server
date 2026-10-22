package team.startup.gwangsan.application.feedback.data;

import team.startup.gwangsan.domain.feedback.model.*;

public record FeedbackSearch(Long memberId, Integer restrictedPlaceId, Integer placeId,
                             FeedbackStatus status, FeedbackType type, Long lastId, int size) {}
