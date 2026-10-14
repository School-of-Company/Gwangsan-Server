package team.startup.gwangsan.application.member.port.out;

import team.startup.gwangsan.application.member.data.AdminScope;
import team.startup.gwangsan.application.member.data.MemberProfile;

import java.util.List;
import java.util.Optional;

public interface MemberProfileQueryPort {

    /** 회원·지점·본부·동을 모두 조인한다. 하나라도 없으면 비어 있다. */
    Optional<MemberProfile> findMyProfile(Long memberId);

    /** 회원·지점을 조인한다. */
    Optional<MemberProfile> findUserProfile(Long memberId);

    Optional<AdminScope> findAdminScope(Long memberId);

    List<MemberProfile> findAll(Integer placeId, Integer headId, String nickname, String placeName);
}
