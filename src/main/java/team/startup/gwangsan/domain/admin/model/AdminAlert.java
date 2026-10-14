package team.startup.gwangsan.domain.admin.model;

import team.startup.gwangsan.domain.admin.model.AlertType;

import java.time.LocalDateTime;

public record AdminAlert(
        Long id,
        AlertType type,
        String title,
        Long sourceId,
        Long requesterId,
        LocalDateTime createdAt
) {
}
