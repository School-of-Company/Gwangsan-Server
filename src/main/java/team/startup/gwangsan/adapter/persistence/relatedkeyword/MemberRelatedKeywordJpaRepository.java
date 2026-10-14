package team.startup.gwangsan.adapter.persistence.relatedkeyword;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.util.List;

public interface MemberRelatedKeywordJpaRepository extends JpaRepository<MemberRelatedKeywordJpaEntity, Long> {
    List<MemberRelatedKeywordJpaEntity> findAllByMemberId(Long memberId);

    void deleteAllByMember(MemberJpaEntity member);

    // 탈퇴 정리 전용: 뒤따르는 clearAutomatically bulk 쿼리에 미flush 삭제가 유실되지 않도록 즉시 실행한다.
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberRelatedKeywordJpaEntity k WHERE k.member.id = :memberId")
    void bulkDeleteAllByMemberId(@Param("memberId") Long memberId);
}
