package team.startup.gwangsan.adapter.persistence.member;

import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MemberDetailCustomRepository {
    PlaceJpaEntity findPlaceByMemberId(Long id);

    Map<Long, String> findPlaceNameMapByMemberIds(Set<Long> memberIds);

    List<MemberDetailJpaEntity> findAllByRoleAndNicknameAndPlaceName(
            Integer placeId,
            Integer headId,
            String nickname,
            String placeName
    );
    MemberDetailJpaEntity findByMemberIdWithMember(Long memberId);

    MemberDetailJpaEntity findByPhoneNumberWithMember(String phoneNumber);

    Optional<MemberDetailJpaEntity> findByMemberIdWithPlaceHeadDong(Long memberId);

    Optional<MemberDetailJpaEntity> findByMemberIdWithMemberAndPlace(Long memberId);
}
