package team.startup.gwangsan.application.chat.port.in;

/**
 * 회원 탈퇴 시 채팅 참조 재지정. 호출자 트랜잭션에 참여한다.
 * 채팅방 구매자 → 판매자 → 메시지 발신자 순으로 bulk update 하며 각 단계는 영속성 컨텍스트를 비운다(clearAutomatically).
 */
public interface ChatMemberCleanupUseCase {
    void reassignMember(Long memberId, Long dummyMemberId);
}
