package team.startup.gwangsan.application.alert.port.in;

/**
 * 회원 탈퇴 시 알림 정리. 호출자 트랜잭션에 참여한다.
 */
public interface AlertMemberCleanupUseCase {
    void reassignSender(Long memberId, Long dummyMemberId);

    void deleteReceipts(Long memberId);
}
