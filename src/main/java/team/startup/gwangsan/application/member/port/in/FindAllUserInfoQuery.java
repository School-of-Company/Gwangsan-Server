package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.application.member.data.MemberProfile;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;

/**
 * 관리자 회원 목록. 본부 관리자는 본부, 지점 관리자는 지점 범위만 조회한다.
 * placeId·headId 는 선택 필터이며 요청자 범위를 벗어나면 NotAllowedUserAccessException(403).
 * placeName 은 기존 클라이언트 호환용 완전 일치 필터다.
 */
public interface FindAllUserInfoQuery {

    List<MemberProfile> query(ActorContext actor, Integer placeId, Integer headId, String nickname, String placeName);
}
