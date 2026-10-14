package team.startup.gwangsan.application.admin.data;

import team.startup.gwangsan.domain.admin.entity.constant.AlertType;

/** 다른 업무가 커밋 후 관리자 알림 생성을 요청하는 이벤트. adapter/event/admin 이 받는다. */
public record CreateAdminAlertEvent(
        AlertType type,
        Long sourceId,
        Long memberId
) {
}
