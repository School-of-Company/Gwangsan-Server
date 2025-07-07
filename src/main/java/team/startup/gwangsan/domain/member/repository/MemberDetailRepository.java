package team.startup.gwangsan.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.custom.MemberDetailCustomRepository;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;
import java.util.Optional;

public interface MemberDetailRepository extends JpaRepository<MemberDetail, Long>, MemberDetailCustomRepository {
    List<MemberDetail> findAllByPlace(Place place);

    Optional<MemberDetail> findByMember(Member member);
}
