package team.startup.gwangsan.adapter.integration.suspend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.application.suspend.port.out.ChangeMemberStatusPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.MemberStatus;

/**
 * bridge: member(PR02) 에 회원 상태 변경 공개 command 가 없어 MemberJpaRepository 를 직접 쓴다.
 * 호출자 트랜잭션 안에서 managed Member 를 바꿔 기존 변경 감지 반영을 유지한다. 공개 command 가 생기면 교체한다.
 */
@Component
@RequiredArgsConstructor
public class MemberStatusBridgeAdapter implements ChangeMemberStatusPort {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public void checkMemberExists(Long memberId) {
        load(memberId);
    }

    @Override
    public void suspend(Long memberId) {
        load(memberId).updateMemberStatus(MemberStatus.SUSPENDED);
    }

    @Override
    public void activate(Long memberId) {
        load(memberId).updateMemberStatus(MemberStatus.ACTIVE);
    }

    private MemberJpaEntity load(Long memberId) {
        return memberJpaRepository.findById(memberId).orElseThrow(NotFoundMemberException::new);
    }
}
