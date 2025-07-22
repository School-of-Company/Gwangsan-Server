package team.startup.gwangsan.domain.alert.presentation.dto.response;

import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.time.LocalDateTime;
import java.util.List;

public record GetAlertResponse(
        Long id,
        String title,
        String content,
        AlertType alertType,
        LocalDateTime createdAt,
        List<GetImageResponse> images
) {
}
