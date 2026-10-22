package team.startup.gwangsan.application.feedback.data;

import team.startup.gwangsan.domain.common.ActorContext;

public record MyFeedbackSearch(ActorContext actor, Long lastId, int size) {
}
