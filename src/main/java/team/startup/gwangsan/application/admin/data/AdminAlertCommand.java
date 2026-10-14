package team.startup.gwangsan.application.admin.data;

import team.startup.gwangsan.domain.common.ActorContext;

/** 관리자 알림 하나를 대상으로 하는 승인·기각·삭제 요청. */
public record AdminAlertCommand(ActorContext actor, Long alertId) {
}
