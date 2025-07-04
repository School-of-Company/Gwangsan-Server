package team.startup.gwangsan.domain.member.repository.custom;

import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;

public interface MemberDetailCustomRepository {
    Place findPlaceByMemberId(Long id);
    List<MemberDetail> findAllWithMember();
}
