package team.startup.gwangsan.adapter.integration.post;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.chat.data.ChatRoomSnapshot;
import team.startup.gwangsan.application.chat.port.in.ChatRoomLookupQuery;
import team.startup.gwangsan.application.chat.port.in.SaveChatSystemMessageUseCase;
import team.startup.gwangsan.application.post.data.ChatRoomView;
import team.startup.gwangsan.application.post.port.out.ProductChatPort;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * post → chat 공개 Port. 시스템 메시지는 호출자의 트랜잭션에서 저장한다.
 */
@Component
@RequiredArgsConstructor
public class PostChatBridgeAdapter implements ProductChatPort {

    private final ChatRoomLookupQuery chatRoomLookupQuery;
    private final SaveChatSystemMessageUseCase saveChatSystemMessageUseCase;

    @Override
    public Optional<ChatRoomView> findRoom(Long roomId) {
        return chatRoomLookupQuery.byRoomId(roomId).map(PostChatBridgeAdapter::toView);
    }

    @Override
    public Optional<ChatRoomView> findRoom(Long productId, Long memberId) {
        return chatRoomLookupQuery.byProductAndMember(productId, memberId).map(PostChatBridgeAdapter::toView);
    }

    @Override
    public boolean hasSentMessage(Long roomId, Long memberId) {
        return chatRoomLookupQuery.hasSentMessage(roomId, memberId);
    }

    @Override
    public Long saveSystemMessage(Long roomId, Long messageId, Long senderId, String content, LocalDateTime createdAt) {
        return saveChatSystemMessageUseCase.save(roomId, messageId, senderId, content, createdAt);
    }

    private static ChatRoomView toView(ChatRoomSnapshot room) {
        return new ChatRoomView(room.roomId(), room.productId(), room.buyerId(), room.sellerId());
    }
}
