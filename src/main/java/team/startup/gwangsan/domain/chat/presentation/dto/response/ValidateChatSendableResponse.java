package team.startup.gwangsan.domain.chat.presentation.dto.response;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.util.List;

public record ValidateChatSendableResponse(List<GetImageResponse> images) {
}
