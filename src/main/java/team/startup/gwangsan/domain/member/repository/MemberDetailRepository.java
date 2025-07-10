package team.startup.gwangsan.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.custom.MemberDetailCustomRepository;

import java.util.List;
import java.util.Optional;

public interface MemberDetailRepository extends JpaRepository<MemberDetail, Long>, MemberDetailCustomRepository {

    Optional<MemberDetail> findByMember(Member member);

    List<MemberDetail> findAllByPlace_Head_Id(Integer headId);

    List<MemberDetail> findAllByPlace_Id(Integer placeId);
}

