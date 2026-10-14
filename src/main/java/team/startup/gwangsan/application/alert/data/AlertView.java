package team.startup.gwangsan.application.alert.data;

import team.startup.gwangsan.domain.alert.model.AlertType;

import java.time.LocalDateTime;
import java.util.List;

public record AlertView(
        Long id,
        String title,
        String content,
        AlertType alertType,
        LocalDateTime createdAt,
        List<AlertImage> images,
        Long sendMemberId,
        Long sourceId
) {
}
