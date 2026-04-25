package team.startup.gwangsan.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.custom.MemberCustomRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberCustomRepository {
    Optional<Member> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNickname(String nickname);

    Optional<Member> findByNickname(String nickname);

    Optional<Member> findByStatusAndId(MemberStatus status, Long id);

    List<Member> findAllByIdIn(List<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.recommender = :dummy WHERE m.recommender = :target")
    void reassignRecommender(@Param("target") Member target, @Param("dummy") Member dummy);
}
