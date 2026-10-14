package team.startup.gwangsan.application.suspend.port.out;

/** 같은 트랜잭션에서 회원 상태를 바꾼다. 회원이 없으면 member 업무의 NotFound 예외를 던진다. */
public interface ChangeMemberStatusPort {

    void checkMemberExists(Long memberId);

    void suspend(Long memberId);

    void activate(Long memberId);
}
