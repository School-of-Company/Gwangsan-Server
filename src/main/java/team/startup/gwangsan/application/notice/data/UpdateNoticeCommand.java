package team.startup.gwangsan.application.notice.data;

import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;

public record UpdateNoticeCommand(ActorContext actor, Long noticeId, String title, String content, List<Long> imageIds) {
}
