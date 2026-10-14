package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;

import java.util.Optional;

/**
 * 회원 상태·역할·가입 처리 공개 command(정지·해제·관리자). 호출자 트랜잭션이 필수다.
 * 존재 확인은 {@link MemberAccountQuery#findById} 를 쓴다.
 */
public interface ManageMemberUseCase {

    /** 회원이 없으면 NotFoundMember. */
    void changeStatus(Long memberId, MemberStatus status);

    /** placeId 가 있으면 상세(없으면 NotFoundMemberDetail)의 지점을 먼저 바꾸고, 회원(없으면 NotFoundMember) 역할을 바꾼다. 지점 존재 확인은 호출자 몫이다. */
    void changeRole(Long memberId, MemberRole role, Integer placeId);

    /** 가입 기각: 회원 상세를 지운다(기존 cascade 로 회원도 지워진다). 상세가 없으면 NotFoundMemberDetail. */
    void rejectSignUp(Long memberId);

    /** 같은 번호의 탈퇴 기록이 있으면 지우고 남긴 광산을 돌려준다. */
    Optional<Integer> takeWithdrawnGwangsan(String phoneNumber);
}
