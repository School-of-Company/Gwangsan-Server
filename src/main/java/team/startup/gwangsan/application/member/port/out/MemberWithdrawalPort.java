package team.startup.gwangsan.application.member.port.out;

import team.startup.gwangsan.application.member.data.WithdrawalTarget;

import java.util.Optional;

/** 회원 업무 자신의 탈퇴 저장 능력. */
public interface MemberWithdrawalPort {

    /** 회원이 없으면 NotFoundMember, 상세가 없으면 NotFoundMemberDetail. */
    WithdrawalTarget loadTarget(Long memberId);

    void saveWithdrawalRecord(String phoneNumber, Integer gwangsan, boolean banned);

    Optional<Long> findDummyMemberId();

    void reassignRecommender(Long memberId, Long dummyMemberId);

    void deleteDetail(Long memberId);

    void deleteMember(Long memberId);
}
