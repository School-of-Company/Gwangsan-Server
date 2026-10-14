package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.application.member.data.MemberProfile;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;

/** 관리자 회원 목록. 본부 관리자는 본부, 지점 관리자는 지점 범위만 조회한다. */
public interface FindAllUserInfoQuery {

    List<MemberProfile> query(ActorContext actor, String nickname, String placeName);
}
