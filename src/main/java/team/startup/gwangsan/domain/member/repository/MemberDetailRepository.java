package team.startup.gwangsan.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.custom.MemberDetailCustomRepository;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;
import java.util.Optional;

public interface MemberDetailRepository extends JpaRepository<MemberDetail, Long>, MemberDetailCustomRepository {
    Optional<MemberDetail> findByMember(Member member);

    List<MemberDetail> findAllByMemberIdIn(List<Long> memberIds);

    List<MemberDetail> findAllByPlace(Place place);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberDetail d WHERE d.member = :member")
    void deleteByMember(@Param("member") Member member);
}

