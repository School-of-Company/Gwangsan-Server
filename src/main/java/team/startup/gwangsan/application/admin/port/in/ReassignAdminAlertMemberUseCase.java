package team.startup.gwangsan.application.admin.port.in;

/** 회원 탈퇴 시 관리자 알림의 요청자/상대 회원을 dummy 회원으로 바꾼다. 호출자 트랜잭션에 참여한다. */
public interface ReassignAdminAlertMemberUseCase {
    void reassign(Long memberId, Long dummyMemberId);
}
