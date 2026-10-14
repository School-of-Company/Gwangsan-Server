package team.startup.gwangsan.application.member.data;

/**
 * 다른 업무가 쓰는 회원 요약. 회원 기준 left join 이라 상세가 없는 회원도 포함되며 그때 상세·지점 값은 null 이다.
 * 상세 기준(inner join) 결과가 필요한 소비자는 {@link #hasDetail()} 로 거른다(place_id 는 NOT NULL 이라 동일 집합).
 */
public record MemberSummary(
        Long memberId,
        String nickname,
        String phoneNumber,
        Long recommenderId,
        String recommenderNickname,
        Integer placeId,
        Integer headId,
        String placeName,
        Integer light
) {
    public boolean hasDetail() {
        return light != null;
    }
}
