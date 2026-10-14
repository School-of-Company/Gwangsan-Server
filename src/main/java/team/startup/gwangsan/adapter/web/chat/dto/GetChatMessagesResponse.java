package team.startup.gwangsan.adapter.web.chat.dto;


import java.util.List;

public record GetChatMessagesResponse(
        GetChatProductDto product,
        List<GetChatMessageDto> messages
) {
}
