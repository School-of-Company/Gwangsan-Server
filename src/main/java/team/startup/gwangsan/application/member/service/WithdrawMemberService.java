package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.data.WithdrawMemberCommand;
import team.startup.gwangsan.application.member.data.WithdrawalTarget;
import team.startup.gwangsan.application.member.port.in.WithdrawMemberUseCase;
import team.startup.gwangsan.application.member.port.out.MemberReferenceCleanupPort;
import team.startup.gwangsan.application.member.port.out.MemberWithdrawalPort;
import team.startup.gwangsan.domain.member.exception.DummyMemberDeletionNotAllowedException;
import team.startup.gwangsan.domain.member.exception.NotFoundDummyMemberException;

@Service
@RequiredArgsConstructor
public class WithdrawMemberService implements WithdrawMemberUseCase {

    private final MemberWithdrawalPort memberWithdrawalPort;
    private final MemberReferenceCleanupPort memberReferenceCleanupPort;

    // 순서는 기존 MemberDeletionServiceImpl 과 같다. 어느 단계든 실패하면 탈퇴 기록까지 전부 rollback 된다.
    @Override
    @Transactional
    public void execute(WithdrawMemberCommand command) {
        Long memberId = command.memberId();
        WithdrawalTarget target = memberWithdrawalPort.loadTarget(memberId);
        memberWithdrawalPort.saveWithdrawalRecord(target.phoneNumber(), target.gwangsan(), command.banned());

        Long dummyId = memberWithdrawalPort.findDummyMemberId()
                .orElseThrow(NotFoundDummyMemberException::new);
        if (memberId.equals(dummyId)) {
            throw new DummyMemberDeletionNotAllowedException();
        }

        memberWithdrawalPort.reassignRecommender(memberId, dummyId);
        memberReferenceCleanupPort.reassignReferences(memberId, dummyId);
        memberReferenceCleanupPort.deletePersonalData(memberId);
        memberWithdrawalPort.deleteDetail(memberId);
        memberWithdrawalPort.deleteMember(memberId);
    }
}
