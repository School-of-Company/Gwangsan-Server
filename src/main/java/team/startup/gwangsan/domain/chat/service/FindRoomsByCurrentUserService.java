package team.startup.gwangsan.domain.chat.service;

import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomsResponse;

import java.util.List;

public interface FindRoomsByCurrentUserService {
    List<GetRoomsResponse> execute();
}
