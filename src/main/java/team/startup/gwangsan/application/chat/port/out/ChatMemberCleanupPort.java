package team.startup.gwangsan.application.chat.port.out;

public interface ChatMemberCleanupPort {
    void reassignMember(Long memberId, Long dummyMemberId);
}
