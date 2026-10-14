package team.startup.gwangsan.application.notice.data;

import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;

// imageIds 는 null 이거나 비어 있으면 이미지 없이 생성한다.
public record CreateNoticeCommand(ActorContext actor, String title, String content, Integer placeId, List<Long> imageIds) {
}
