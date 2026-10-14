package team.startup.gwangsan.application.alert.port.out;

public interface AlertMemberCleanupPort {
    void reassignSender(Long memberId, Long dummyMemberId);

    void deleteReceipts(Long memberId);
}
