package team.startup.gwangsan.domain.chat.presentation.dto;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.util.List;

public record GetChatProductDto(
        Long id,
        String title,
        List<GetImageResponse> images,
        boolean isSeller,
        boolean isCompletable
)  {
}
