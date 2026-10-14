package team.startup.gwangsan.domain.relatedkeyword.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;

import java.util.List;

public interface MemberRelatedKeywordRepository extends JpaRepository<MemberRelatedKeyword, Long> {
    List<MemberRelatedKeyword> findAllByMember(MemberJpaEntity member);

    List<MemberRelatedKeyword> findAllByMemberId(Long memberId);

    void deleteAllByMember(MemberJpaEntity member);

    // 탈퇴 정리 전용: 뒤따르는 clearAutomatically bulk 쿼리에 미flush 삭제가 유실되지 않도록 즉시 실행한다.
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberRelatedKeyword k WHERE k.member.id = :memberId")
    void bulkDeleteAllByMemberId(@Param("memberId") Long memberId);
}
