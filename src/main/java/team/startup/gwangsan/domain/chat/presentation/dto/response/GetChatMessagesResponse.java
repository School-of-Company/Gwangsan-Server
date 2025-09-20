package team.startup.gwangsan.domain.chat.presentation.dto.response;

import team.startup.gwangsan.domain.chat.presentation.dto.GetChatMessageDto;
import team.startup.gwangsan.domain.chat.presentation.dto.GetChatProductDto;

import java.util.List;

public record GetChatMessagesResponse(
        GetChatProductDto product,
        List<GetChatMessageDto> messages
) {
}
