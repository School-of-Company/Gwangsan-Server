package team.startup.gwangsan.global.event;

import team.startup.gwangsan.domain.admin.entity.constant.AlertType;

public record CreateAdminAlertEvent(
        AlertType type,
        Long sourceId,
        Long memberId
) {
}
