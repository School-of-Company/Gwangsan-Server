package team.startup.gwangsan.adapter.persistence.chat;

import team.startup.gwangsan.application.chat.data.SaveChatMessageCommand;
import team.startup.gwangsan.domain.chat.model.StoredChatMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ChatMessageCustomRepository {

    List<ChatMessageJpaEntity> findChatMessageByRoomIdWithCursorPaging(Long roomId, LocalDateTime lastCreatedAt, Long lastMessageId, int limit);

    void readMessage(Long roomId, Long lastMessageId, Long readerId);

    boolean insertIfAbsent(SaveChatMessageCommand message);

    Optional<StoredChatMessage> findStoredMessage(Long messageId);

    Set<Long> findExistingImageIds(List<Long> imageIds);
}
