package team.startup.gwangsan.adapter.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;
import java.util.Optional;

public interface MemberDetailJpaRepository extends JpaRepository<MemberDetailJpaEntity, Long>, MemberDetailCustomRepository {
    Optional<MemberDetailJpaEntity> findByMember(MemberJpaEntity member);

    @EntityGraph(attributePaths = "place")
    List<MemberDetailJpaEntity> findAllByMemberIdIn(List<Long> memberIds);

    List<MemberDetailJpaEntity> findAllByPlace(Place place);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberDetail d WHERE d.member = :member")
    void deleteByMember(@Param("member") MemberJpaEntity member);
}
