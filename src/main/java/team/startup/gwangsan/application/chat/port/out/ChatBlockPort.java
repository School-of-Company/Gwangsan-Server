package team.startup.gwangsan.application.chat.port.out;

public interface ChatBlockPort {
    /** 어느 한쪽이라도 차단했으면 차단 오류(403). */
    void validateNotBlocked(Long memberId, Long otherMemberId);
}
