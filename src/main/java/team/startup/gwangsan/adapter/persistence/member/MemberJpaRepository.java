package team.startup.gwangsan.adapter.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.member.model.MemberStatus;

import java.util.List;
import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long>, MemberCustomRepository {
    Optional<MemberJpaEntity> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNickname(String nickname);

    Optional<MemberJpaEntity> findByNickname(String nickname);

    Optional<MemberJpaEntity> findByStatusAndId(MemberStatus status, Long id);

    List<MemberJpaEntity> findAllByIdIn(List<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.recommender = :dummy WHERE m.recommender = :target")
    void reassignRecommender(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}
