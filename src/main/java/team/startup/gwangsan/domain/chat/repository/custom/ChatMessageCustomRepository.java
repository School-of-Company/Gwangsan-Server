package team.startup.gwangsan.domain.chat.repository.custom;

import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ChatMessageCustomRepository {
    List<ChatMessage> findChatMessageByRoomIdWithCursorPaging(Long roomId, LocalDateTime lastCreatedAt, Long lastMessageId, int limit);

    void readMessage(Long roomId, Long lastMessageId, Long readerId);

    boolean insertIfAbsent(ChatMessage message);

    Optional<StoredMessage> findStoredMessage(Long messageId);

    Set<Long> findExistingImageIds(List<Long> imageIds);

    record StoredMessage(Long messageId, Long roomId, Long senderId, String content,
                         MessageType messageType, LocalDateTime createdAt, boolean checked,
                         List<GetImageResponse> images) { }
}
