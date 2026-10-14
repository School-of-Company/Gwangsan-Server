package team.startup.gwangsan.application.post.data;

import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;

/**
 * size 가 null 이면 기존 비페이지 목록, 있으면 id 내림차순 cursor 페이지다. 두 경로는 쿼리가 다르다.
 */
public record MemberProductsCriteria(Long memberId, Type type, Mode mode, Long lastId, Integer size, Boolean completed) {
}
