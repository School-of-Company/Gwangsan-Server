package team.startup.gwangsan.domain.member.repository.custom;

import team.startup.gwangsan.domain.place.entity.Place;

public interface MemberDetailCustomRepository {
    Place findPlaceByMemberId(Long id);
}
