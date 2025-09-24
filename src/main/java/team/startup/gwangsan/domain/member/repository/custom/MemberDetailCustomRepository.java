package team.startup.gwangsan.domain.member.repository.custom;

import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MemberDetailCustomRepository {
    Place findPlaceByMemberId(Long id);

    Map<Long, String> findPlaceNameMapByMemberIds(Set<Long> memberIds);

    List<MemberDetail> findAllByNicknameAndPlaceNameAndPlaceIdAndHeadId(String nickname, String placeName, Integer placeId, Integer headId);

    MemberDetail findByMemberIdWithMember(Long memberId);

    MemberDetail findByPhoneNumberWithMember(String phoneNumber);
}
