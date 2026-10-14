package team.startup.gwangsan.adapter.integration.post;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.post.data.ChatRoomView;
import team.startup.gwangsan.application.post.port.out.ProductChatPort;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * chat 업무(PR08) 미전환 기간의 bridge. 시스템 메시지는 호출자의 트랜잭션에서 저장한다. PR08 의 공개 Port 로 교체한다.
 */
@Component
@RequiredArgsConstructor
public class PostChatBridgeAdapter implements ProductChatPort {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<ChatRoomView> findRoom(Long roomId) {
        return chatRoomRepository.findChatRoomByRoomId(roomId).map(PostChatBridgeAdapter::toView);
    }

    @Override
    public Optional<ChatRoomView> findRoom(Long productId, Long memberId) {
        return chatRoomRepository.findByProductIdAndMember(productId, entityManager.getReference(MemberJpaEntity.class, memberId))
                .map(PostChatBridgeAdapter::toView);
    }

    @Override
    public boolean hasSentMessage(Long roomId, Long memberId) {
        return chatMessageRepository.existsByRoomAndSenderId(entityManager.getReference(ChatRoom.class, roomId), memberId);
    }

    @Override
    public Long saveSystemMessage(Long roomId, Long messageId, Long senderId, String content, LocalDateTime createdAt) {
        return chatMessageRepository.save(ChatMessage.builder()
                .id(messageId)
                .room(entityManager.getReference(ChatRoom.class, roomId))
                .sender(entityManager.getReference(MemberJpaEntity.class, senderId))
                .content(content)
                .messageType(MessageType.SYSTEM)
                .checked(true)
                .createdAt(createdAt)
                .build()).getId();
    }

    private static ChatRoomView toView(ChatRoom room) {
        return new ChatRoomView(room.getId(), room.getProduct().getId(), room.getBuyer().getId(), room.getSeller().getId());
    }
}
