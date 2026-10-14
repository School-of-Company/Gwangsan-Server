package team.startup.gwangsan.application.admin.data;

import team.startup.gwangsan.domain.admin.model.AlertType;
import team.startup.gwangsan.domain.common.ActorContext;

public record AdminAlertSearch(ActorContext actor, Integer placeId, AlertType type) {
}
