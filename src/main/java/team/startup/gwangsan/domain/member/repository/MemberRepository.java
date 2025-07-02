package team.startup.gwangsan.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNickname(String nickname);

    Optional<Member> findByNickname(String nickname);

    Optional<Member> findByStatusAndId(MemberStatus status, Long id);

    List<Member> findAllByStatus(MemberStatus status);
}
