package team.startup.gwangsan.application.admin.port.in;

import team.startup.gwangsan.domain.admin.entity.constant.AlertType;

/** 원본 요청이 다른 경로로 처리됐을 때 남은 관리자 알림을 지운다. 호출자 트랜잭션에 참여한다. */
public interface RemoveAdminAlertUseCase {
    void removeBySource(AlertType type, Long sourceId);
}
