package team.startup.gwangsan.domain.chat.service;

import team.startup.gwangsan.domain.chat.presentation.dto.response.GetChatMessageResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface FindChatMessageByRoomIdService {
    List<GetChatMessageResponse> execute(Long roomId, LocalDateTime lastCreatedAt, Long lastMessageId, int limit);
}
