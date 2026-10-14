package team.startup.gwangsan.application.post.port.out;

import team.startup.gwangsan.application.post.data.ChatRoomView;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProductChatPort {
    Optional<ChatRoomView> findRoom(Long roomId);

    /** 상품의 채팅방 중 회원이 참여한 방. */
    Optional<ChatRoomView> findRoom(Long productId, Long memberId);

    boolean hasSentMessage(Long roomId, Long memberId);

    /** 시스템 메시지를 같은 트랜잭션에서 저장하고 메시지 ID 를 돌려준다. */
    Long saveSystemMessage(Long roomId, Long messageId, Long senderId, String content, LocalDateTime createdAt);
}
