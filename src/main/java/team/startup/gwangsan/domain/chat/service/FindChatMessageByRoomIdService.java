package team.startup.gwangsan.domain.chat.service;

import team.startup.gwangsan.domain.chat.presentation.dto.response.GetChatMessagesResponse;

import java.time.LocalDateTime;

public interface FindChatMessageByRoomIdService {
    GetChatMessagesResponse execute(Long roomId, LocalDateTime lastCreatedAt, Long lastMessageId, int limit);
}
