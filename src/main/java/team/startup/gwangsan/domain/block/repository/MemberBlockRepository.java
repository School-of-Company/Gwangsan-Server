package team.startup.gwangsan.domain.block.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.block.entity.MemberBlock;
import team.startup.gwangsan.domain.block.repository.custom.MemberBlockCustomRepository;
import team.startup.gwangsan.domain.member.entity.Member;

import java.util.List;
import java.util.Optional;

public interface MemberBlockRepository extends JpaRepository<MemberBlock, Long>, MemberBlockCustomRepository {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<MemberBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @EntityGraph(attributePaths = "blocked")
    List<MemberBlock> findAllByBlocker(Member blocker);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberBlock b WHERE b.blocker = :member OR b.blocked = :member")
    void deleteAllByBlockerOrBlocked(@Param("member") Member member);
}
