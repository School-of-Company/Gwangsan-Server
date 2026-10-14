package team.startup.gwangsan.application.notice.data;

// 공지가 필요로 하는 지점 정보. headId 는 본부가 없는 지점이면 null 이다.
public record NoticePlace(Integer placeId, String name, Integer headId) {
}
