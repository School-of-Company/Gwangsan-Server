package team.startup.gwangsan.application.notice.data;

import team.startup.gwangsan.domain.common.ActorContext;

// lastId 가 null 이면 첫 페이지다.
public record NoticeListCriteria(ActorContext actor, Long lastId, int size) {
}
