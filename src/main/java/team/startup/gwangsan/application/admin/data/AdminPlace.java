package team.startup.gwangsan.application.admin.data;

/** 관리자 또는 회원의 소속 지점. 지점이 없으면 placeId/headId 가 null 이다. */
public record AdminPlace(Integer placeId, Integer headId) {
}
