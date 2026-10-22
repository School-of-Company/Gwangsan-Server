package team.startup.gwangsan.application.feedback.port.out;

public interface AdminPlaceLookupPort {

    /** PLACE_ADMIN 본인의 지점. 상세가 없으면 NotFoundMemberDetail. */
    Integer ownPlaceId(Long memberId);

    /** HEAD_ADMIN 이 명시한 place_id 필터 검증. 없으면 PlaceNotFound. */
    Integer requirePlace(Integer placeId);
}
