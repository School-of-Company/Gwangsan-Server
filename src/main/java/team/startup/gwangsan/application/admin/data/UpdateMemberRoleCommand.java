package team.startup.gwangsan.application.admin.data;

import team.startup.gwangsan.domain.common.ActorContext;

public record UpdateMemberRoleCommand(ActorContext actor, Long memberId, ActorContext.Role role, Integer placeId) {
}
