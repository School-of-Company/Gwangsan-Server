package team.startup.gwangsan.domain.chat.repository.custom;

import team.startup.gwangsan.domain.chat.entity.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageCustomRepository {
    List<ChatMessage> findChatMessageByRoomIdsWithCursorPaging(List<Long> roomIds, LocalDateTime lastCreatedAt, Long lastMessageId, int limit);
}
