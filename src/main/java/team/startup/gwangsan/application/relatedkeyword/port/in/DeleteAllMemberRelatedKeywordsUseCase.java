package team.startup.gwangsan.application.relatedkeyword.port.in;

/**
 * 회원 탈퇴 정리 전용. 회원의 관심 키워드 매핑을 즉시 실행되는 bulk 삭제로 지우고 영속성 컨텍스트를 비운다.
 * 호출자 트랜잭션에 참여하며, 실행 뒤 그 트랜잭션에서 관리 중이던 엔티티는 detached 상태가 된다.
 * 내 정보 수정처럼 같은 컨텍스트의 dirty 변경을 보존해야 하는 경로는 이 UseCase 를 쓰지 않는다.
 */
public interface DeleteAllMemberRelatedKeywordsUseCase {
    void execute(Long memberId);
}
