package team.startup.gwangsan.application.member.data;

import team.startup.gwangsan.domain.common.ActorContext;

public record UpdateMyPlaceCommand(ActorContext actor, Integer placeId) {
}
