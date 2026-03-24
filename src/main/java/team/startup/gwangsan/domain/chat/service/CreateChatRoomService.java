package team.startup.gwangsan.domain.chat.service;

import team.startup.gwangsan.domain.chat.presentation.dto.response.CreateChatRoomResponse;

public interface CreateChatRoomService {
    CreateChatRoomResponse execute(Long productId);
}
