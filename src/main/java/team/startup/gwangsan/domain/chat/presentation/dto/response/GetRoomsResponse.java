package team.startup.gwangsan.domain.chat.presentation.dto.response;

import team.startup.gwangsan.domain.chat.entity.constant.MessageType;

import java.time.LocalDateTime;

public record GetRoomsResponse(
        Long roomId,
        GetRoomMemberResponse member,
        Long messageId,
        String lastMessage,
        MessageType lastMessageType,
        LocalDateTime lastMessageTime,
        Long unreadMessageCount
) {
}
