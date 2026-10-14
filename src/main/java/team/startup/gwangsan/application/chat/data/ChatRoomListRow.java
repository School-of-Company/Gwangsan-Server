package team.startup.gwangsan.application.chat.data;

import team.startup.gwangsan.domain.chat.model.MessageType;

import java.time.LocalDateTime;

/** 방 목록 한 줄. 최신 메시지/미읽음은 삭제·SYSTEM 메시지를 제외한 값이다. */
public record ChatRoomListRow(
        Long roomId,
        Long opponentId,
        String opponentNickname,
        Long messageId,
        String lastMessage,
        MessageType lastMessageType,
        LocalDateTime lastMessageTime,
        Long unreadMessageCount,
        Long productId
) {
}
