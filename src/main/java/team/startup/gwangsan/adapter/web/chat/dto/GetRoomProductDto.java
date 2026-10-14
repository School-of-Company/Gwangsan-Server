package team.startup.gwangsan.adapter.web.chat.dto;

import team.startup.gwangsan.adapter.web.image.dto.GetImageResponse;

import java.util.List;

public record GetRoomProductDto(
        Long productId,
        String title,
        boolean isCompleted,
        boolean isReserved,
        boolean isDeleted,
        List<GetImageResponse> images
) {
}
