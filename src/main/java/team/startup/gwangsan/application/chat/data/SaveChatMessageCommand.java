package team.startup.gwangsan.application.chat.data;

import team.startup.gwangsan.domain.chat.model.MessageType;

import java.time.LocalDateTime;
import java.util.List;

/** 채팅 서버가 만든 messageId 로 저장한다. imageIds 는 null 일 수 있다. */
public record SaveChatMessageCommand(
        Long messageId,
        Long roomId,
        Long senderId,
        String content,
        List<Long> imageIds,
        MessageType messageType,
        LocalDateTime createdAt
) {
}
