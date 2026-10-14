package team.startup.gwangsan.application.place.data;

/** 지점 조회 결과. headId 는 본점 미지정 지점이면 null 이다. */
public record PlaceResult(Integer id, String name, Integer headId) {
}
