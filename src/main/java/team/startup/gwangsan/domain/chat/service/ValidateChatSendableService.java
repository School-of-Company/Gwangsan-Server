package team.startup.gwangsan.domain.chat.service;

import team.startup.gwangsan.domain.chat.presentation.dto.response.ValidateChatSendableResponse;

import java.util.List;

public interface ValidateChatSendableService {
    ValidateChatSendableResponse execute(Long roomId, List<String> imageIds);
}
