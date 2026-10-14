package team.startup.gwangsan.application.alert.data;

import team.startup.gwangsan.domain.alert.model.AlertType;

import java.util.List;

public record CreateAlertForMembersCommand(
        AlertType type,
        Long sourceId,
        List<Long> memberIds
) {
}
