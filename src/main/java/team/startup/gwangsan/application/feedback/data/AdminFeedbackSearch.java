package team.startup.gwangsan.application.feedback.data;

import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;

/** placeId 는 HEAD_ADMIN 이 임의로 좁혀 볼 때만 쓰는 선택 필터다. PLACE_ADMIN 은 이 값을 무시하고 자기 지점 범위를 쓴다. */
public record AdminFeedbackSearch(
        ActorContext actor,
        FeedbackStatus status,
        FeedbackType type,
        Integer placeId,
        Long lastId,
        int size
) {
}
