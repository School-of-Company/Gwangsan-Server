package team.startup.gwangsan.application.block.port.in;

/** 회원 탈퇴 정리: 회원이 차단했거나 차단당한 행을 bulk 삭제한다. 호출자 트랜잭션에 참여한다. */
public interface DeleteMemberBlocksUseCase {
    void deleteAllOf(Long memberId);
}
