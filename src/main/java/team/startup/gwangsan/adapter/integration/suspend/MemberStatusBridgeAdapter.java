package team.startup.gwangsan.adapter.integration.suspend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.member.port.in.ManageMemberUseCase;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.suspend.port.out.ChangeMemberStatusPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.MemberStatus;

/**
 * suspend → member(PR02) 공개 Port 연결. 존재 확인은 MemberAccountQuery, 상태 변경은 ManageMemberUseCase(MANDATORY)로
 * 정지·해제 유스케이스 트랜잭션에 참여한다(정지 이력과 상태가 함께 커밋·rollback).
 */
@Component
@RequiredArgsConstructor
public class MemberStatusBridgeAdapter implements ChangeMemberStatusPort {

    private final MemberAccountQuery memberAccountQuery;
    private final ManageMemberUseCase manageMemberUseCase;

    @Override
    public void checkMemberExists(Long memberId) {
        memberAccountQuery.findById(memberId).orElseThrow(NotFoundMemberException::new);
    }

    @Override
    public void suspend(Long memberId) {
        manageMemberUseCase.changeStatus(memberId, MemberStatus.SUSPENDED);
    }

    @Override
    public void activate(Long memberId) {
        manageMemberUseCase.changeStatus(memberId, MemberStatus.ACTIVE);
    }
}
