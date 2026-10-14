package team.startup.gwangsan.application.alert.data;

import team.startup.gwangsan.domain.alert.model.AlertType;

/**
 * 기존 CreateAlertEvent 와 같은 입력. sourceId 의 의미는 유형마다 다르다.
 */
public record CreateAlertFromSourceCommand(
        AlertType type,
        Long sourceId,
        Long memberId,
        Long suspendId
) {
}
