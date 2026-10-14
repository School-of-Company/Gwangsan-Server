package team.startup.gwangsan.application.notice.data;

import team.startup.gwangsan.domain.common.ActorContext;

public record DeleteNoticeCommand(ActorContext actor, Long noticeId) {
}
