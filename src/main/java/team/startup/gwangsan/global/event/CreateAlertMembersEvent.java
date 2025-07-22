package team.startup.gwangsan.global.event;

import team.startup.gwangsan.domain.alert.entity.constant.AlertType;

import java.util.List;

public record CreateAlertMembersEvent(
        Long sourceId,
        List<Long> memberIds,
        AlertType alertType
) {
}
