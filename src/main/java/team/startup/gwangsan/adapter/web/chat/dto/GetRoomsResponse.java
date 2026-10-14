package team.startup.gwangsan.adapter.web.chat.dto;

import team.startup.gwangsan.domain.chat.model.MessageType;

import java.time.LocalDateTime;

public record GetRoomsResponse(
        Long roomId,
        GetRoomMemberResponse member,
        Long messageId,
        String lastMessage,
        MessageType lastMessageType,
        LocalDateTime lastMessageTime,
        Long unreadMessageCount,
        GetRoomProductDto product
) {
}
