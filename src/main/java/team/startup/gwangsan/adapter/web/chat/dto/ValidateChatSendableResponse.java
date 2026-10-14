package team.startup.gwangsan.adapter.web.chat.dto;

import team.startup.gwangsan.adapter.web.image.dto.GetImageResponse;

import java.util.List;

public record ValidateChatSendableResponse(List<GetImageResponse> images) {
}
