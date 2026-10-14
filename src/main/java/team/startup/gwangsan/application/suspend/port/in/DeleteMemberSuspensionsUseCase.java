package team.startup.gwangsan.application.suspend.port.in;

/** 회원 탈퇴 정리: 회원의 정지 이력을 bulk 삭제한다. 호출자 트랜잭션에 참여한다. */
public interface DeleteMemberSuspensionsUseCase {
    void deleteAllOf(Long memberId);
}
