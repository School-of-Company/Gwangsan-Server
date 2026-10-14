package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.application.member.data.MemberAccount;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 회원 계정 공개 조회. auth·sms 와 보안 adapter 가 사용한다. */
public interface MemberAccountQuery {

    Optional<MemberAccount> findById(Long memberId);

    Optional<MemberAccount> findByNickname(String nickname);

    Optional<MemberAccount> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNickname(String nickname);

    /** 강제 탈퇴 이력이 있는 번호인지. */
    boolean isBannedPhoneNumber(String phoneNumber);

    /** 주어진 ID 중 존재하는 회원 ID. 빈 입력이면 쿼리 없음, 아니면 1 쿼리. 상태 필터 없음, 순서 보장 없음. */
    List<Long> findExistingIds(Collection<Long> memberIds);
}
