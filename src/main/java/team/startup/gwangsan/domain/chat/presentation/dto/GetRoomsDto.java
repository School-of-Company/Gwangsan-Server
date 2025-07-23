package team.startup.gwangsan.domain.chat.presentation.dto;

import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomMemberResponse;

import java.time.LocalDateTime;

public record GetRoomsDto(
        Long roomId,
        GetRoomMemberResponse member,
        Long messageId,
        String lastMessage,
        MessageType lastMessageType,
        LocalDateTime lastMessageTime,
        Long unreadMessageCount,
        Long productId
) {
}
