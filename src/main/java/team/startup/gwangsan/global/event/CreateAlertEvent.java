package team.startup.gwangsan.global.event;

import team.startup.gwangsan.domain.alert.entity.constant.AlertType;

public record CreateAlertEvent(
        Long sourceId,
        Long memberId,
        AlertType alertType,
        Long suspendId
) {
    public CreateAlertEvent(Long sourceId, Long memberId, AlertType alertType) {
        this(sourceId, memberId, alertType, null);
    }
}
