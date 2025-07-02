package team.startup.gwangsan.domain.member.repository.custom;

import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;

public interface MemberCustomRepository {
    List<Member> findByStatusAndPlace(MemberStatus status, Place place);
}
