package team.startup.gwangsan.domain.chat.service;

import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomIdResponse;

public interface FindRoomIdByProductIdService {
    GetRoomIdResponse execute(Long productId);
}
