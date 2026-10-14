package team.startup.gwangsan.application.block.port.out;

public interface LoadBlockTargetPort {

    /** 회원이 없으면 member 업무의 NotFound 예외를 던진다. */
    void checkMemberExists(Long memberId);
}
