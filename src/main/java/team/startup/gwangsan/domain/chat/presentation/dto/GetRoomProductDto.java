package team.startup.gwangsan.domain.chat.presentation.dto;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.util.List;

public record GetRoomProductDto(
        Long productId,
        String title,
        List<GetImageResponse> images
) {
}
