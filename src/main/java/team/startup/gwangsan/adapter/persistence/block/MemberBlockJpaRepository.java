package team.startup.gwangsan.adapter.persistence.block;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.block.custom.MemberBlockCustomRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.util.List;
import java.util.Optional;

public interface MemberBlockJpaRepository extends JpaRepository<MemberBlockJpaEntity, Long>, MemberBlockCustomRepository {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<MemberBlockJpaEntity> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @EntityGraph(attributePaths = "blocked")
    List<MemberBlockJpaEntity> findAllByBlockerId(Long blockerId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberBlockJpaEntity b WHERE b.blocker = :member OR b.blocked = :member")
    void deleteAllByBlockerOrBlocked(@Param("member") MemberJpaEntity member);
}
