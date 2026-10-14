package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.application.member.data.WithdrawMemberCommand;

/** 회원 탈퇴. 탈퇴 기록과 모든 업무 참조 재지정/삭제를 호출자 트랜잭션 하나에서 수행한다. */
public interface WithdrawMemberUseCase {
    void execute(WithdrawMemberCommand command);
}
