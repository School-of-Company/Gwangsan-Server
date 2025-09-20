package team.startup.gwangsan.domain.chat.repository.custom;

import team.startup.gwangsan.domain.chat.entity.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageCustomRepository {
    List<ChatMessage> findChatMessageByRoomIdWithCursorPaging(Long roomId, LocalDateTime lastCreatedAt, Long lastMessageId, int limit);

    List<ChatMessage> findUnreadMessages(Long roomId, Long lastMessageId, Long readerId);
}
