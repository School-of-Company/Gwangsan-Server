package team.startup.gwangsan.domain.chat.service;

import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.presentation.dto.response.SaveChatMessageResponse;

import java.util.List;

public interface SaveChatMessageService {
    SaveChatMessageResponse execute(Long roomId, String content, List<Long> imageIds, MessageType messageType);
}
