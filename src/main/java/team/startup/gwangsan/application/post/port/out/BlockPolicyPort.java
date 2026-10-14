package team.startup.gwangsan.application.post.port.out;

public interface BlockPolicyPort {
    /** 두 회원 사이에 차단이 있으면 예외. */
    void validateNotBlocked(Long actorId, Long targetId);
}
