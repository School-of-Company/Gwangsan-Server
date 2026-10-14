package team.startup.gwangsan.application.chat.data;

import team.startup.gwangsan.domain.common.ActorContext;

import java.time.LocalDateTime;

public record LoadChatRoomMessagesCommand(
        ActorContext actor,
        Long roomId,
        LocalDateTime lastCreatedAt,
        Long lastMessageId,
        int limit
) {
}
