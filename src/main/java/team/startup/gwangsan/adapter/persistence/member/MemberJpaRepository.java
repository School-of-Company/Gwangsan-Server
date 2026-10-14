package team.startup.gwangsan.adapter.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.domain.member.model.MemberStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long>, MemberCustomRepository {
    Optional<MemberJpaEntity> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNickname(String nickname);

    Optional<MemberJpaEntity> findByNickname(String nickname);

    Optional<MemberJpaEntity> findByStatusAndId(MemberStatus status, Long id);

    List<MemberJpaEntity> findAllByIdIn(List<Long> ids);

    @Query("SELECT m.id FROM Member m WHERE m.id IN :ids")
    List<Long> findExistingIds(@Param("ids") Collection<Long> ids);

    // 회원 기준 left join: 상세·지점·추천인이 없어도 회원 행은 남는다.
    @Query("""
            SELECT new team.startup.gwangsan.application.member.data.MemberSummary(
                m.id, m.nickname, m.phoneNumber, r.id, r.nickname, p.id, h.id, p.name, d.light)
            FROM Member m
            LEFT JOIN m.recommender r
            LEFT JOIN MemberDetail d ON d.id = m.id
            LEFT JOIN d.place p
            LEFT JOIN p.head h
            WHERE m.id IN :ids
            """)
    List<MemberSummary> findSummaries(@Param("ids") Collection<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.recommender = :dummy WHERE m.recommender = :target")
    void reassignRecommender(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}
