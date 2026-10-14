package team.startup.gwangsan.adapter.persistence.chat;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.application.chat.port.out.ChatMemberCleanupPort;

@Component
@RequiredArgsConstructor
public class ChatMemberCleanupPersistenceAdapter implements ChatMemberCleanupPort {

    private final EntityManager entityManager;
    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final ChatMessageJpaRepository chatMessageJpaRepository;

    @Override
    public void reassignMember(Long memberId, Long dummyMemberId) {
        // 각 bulk update 가 clearAutomatically 로 컨텍스트를 비우므로 참조는 매번 새로 얻는다.
        chatRoomJpaRepository.reassignBuyer(memberRef(memberId), memberRef(dummyMemberId));
        chatRoomJpaRepository.reassignSeller(memberRef(memberId), memberRef(dummyMemberId));
        chatMessageJpaRepository.reassignSender(memberRef(memberId), memberRef(dummyMemberId));
    }

    private MemberJpaEntity memberRef(Long memberId) {
        return entityManager.getReference(MemberJpaEntity.class, memberId);
    }
}
