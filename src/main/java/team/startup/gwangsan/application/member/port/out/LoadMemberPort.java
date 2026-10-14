package team.startup.gwangsan.application.member.port.out;

import team.startup.gwangsan.domain.member.model.Member;
import team.startup.gwangsan.domain.member.model.MemberDetail;

import java.util.Optional;

public interface LoadMemberPort {

    Optional<Member> findById(Long memberId);

    Optional<Member> findByNickname(String nickname);

    Optional<Member> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNickname(String nickname);

    Optional<MemberDetail> findDetail(Long memberId);

    boolean isBannedPhoneNumber(String phoneNumber);
}
