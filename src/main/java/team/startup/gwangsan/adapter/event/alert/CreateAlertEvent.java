package team.startup.gwangsan.adapter.event.alert;

import team.startup.gwangsan.domain.alert.model.AlertType;

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
