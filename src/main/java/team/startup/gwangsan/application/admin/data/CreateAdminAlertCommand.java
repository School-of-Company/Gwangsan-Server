package team.startup.gwangsan.application.admin.data;

import team.startup.gwangsan.domain.admin.model.AlertType;

public record CreateAdminAlertCommand(AlertType type, Long sourceId, Long requesterId) {
}
