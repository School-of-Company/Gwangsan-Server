package team.startup.gwangsan.domain.member.repository.custom;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.projection.FindAllUserDto;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MemberDetailCustomRepository {
    Place findPlaceByMemberId(Long id);

    Map<Long, String> findPlaceNameMapByMemberIds(Set<Long> memberIds);

    List<MemberDetail> findAllByRoleAndNicknameAndPlaceName(
            Integer placeId,
            Integer headId,
            String nickname,
            String placeName
    );
    MemberDetail findByMemberIdWithMember(Long memberId);

    MemberDetail findByPhoneNumberWithMember(String phoneNumber);

    Optional<MemberDetail> findByMemberIdWithPlaceHeadDong(Long memberId);

    Slice<FindAllUserDto> findAllUserSlice(
            Integer placeId,
            Integer headId,
            String nickname,
            String placeName,
            Pageable pageable
    );
}