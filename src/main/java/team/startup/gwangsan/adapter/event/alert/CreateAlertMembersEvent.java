package team.startup.gwangsan.adapter.event.alert;

import team.startup.gwangsan.domain.alert.model.AlertType;

import java.util.List;

public record CreateAlertMembersEvent(
        Long sourceId,
        List<Long> memberIds,
        AlertType alertType
) {
}
