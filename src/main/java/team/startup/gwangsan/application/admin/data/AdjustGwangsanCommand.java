package team.startup.gwangsan.application.admin.data;

import team.startup.gwangsan.domain.common.ActorContext;

public record AdjustGwangsanCommand(ActorContext actor, Long memberId, Integer gwangsan) {
}
