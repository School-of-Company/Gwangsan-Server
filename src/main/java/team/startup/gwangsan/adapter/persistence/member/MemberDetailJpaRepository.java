package team.startup.gwangsan.adapter.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;

import java.util.List;
import java.util.Optional;

public interface MemberDetailJpaRepository extends JpaRepository<MemberDetailJpaEntity, Long>, MemberDetailCustomRepository {
    Optional<MemberDetailJpaEntity> findByMember(MemberJpaEntity member);

    @EntityGraph(attributePaths = "place")
    List<MemberDetailJpaEntity> findAllByMemberIdIn(List<Long> memberIds);

    List<MemberDetailJpaEntity> findAllByPlace(PlaceJpaEntity place);

    @Query("SELECT d.id FROM MemberDetail d WHERE d.place.id = :placeId")
    List<Long> findIdsByPlaceId(@Param("placeId") Integer placeId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE MemberDetail d SET d.gwangsan = d.gwangsan + :delta WHERE d.id = :memberId")
    int addGwangsan(@Param("memberId") Long memberId, @Param("delta") int delta);

    @Query(value = "SELECT gwangsan FROM tbl_member_detail WHERE member_id = :memberId", nativeQuery = true)
    Optional<Integer> findGwangsan(@Param("memberId") Long memberId);

    @Query(value = "SELECT gwangsan FROM tbl_member_detail WHERE member_id = :memberId FOR UPDATE", nativeQuery = true)
    Optional<Integer> findGwangsanForUpdate(@Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberDetail d WHERE d.member = :member")
    void deleteByMember(@Param("member") MemberJpaEntity member);
}
