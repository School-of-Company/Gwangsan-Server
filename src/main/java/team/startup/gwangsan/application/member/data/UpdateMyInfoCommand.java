package team.startup.gwangsan.application.member.data;

import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;

public record UpdateMyInfoCommand(ActorContext actor, String nickname, String description, List<String> specialties) {
}
