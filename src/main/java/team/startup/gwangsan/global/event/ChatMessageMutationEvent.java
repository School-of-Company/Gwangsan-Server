package team.startup.gwangsan.global.event;

import team.startup.gwangsan.domain.chat.entity.constant.MessageType;

import java.time.LocalDateTime;

public record ChatMessageMutationEvent(
        Kind kind, Long roomId, Long messageId, String content, LocalDateTime editedAt,
        boolean roomListChanged, LatestMessage latestMessage
) {
    public enum Kind { UPDATED, DELETED }

    public record LatestMessage(Long messageId, String content, MessageType messageType,
                                LocalDateTime createdAt, LocalDateTime editedAt) {
    }
}
