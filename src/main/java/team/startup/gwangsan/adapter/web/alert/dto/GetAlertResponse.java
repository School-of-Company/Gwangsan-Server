package team.startup.gwangsan.adapter.web.alert.dto;

import team.startup.gwangsan.application.alert.data.AlertView;
import team.startup.gwangsan.domain.alert.model.AlertType;

import java.time.LocalDateTime;
import java.util.List;

public record GetAlertResponse(
        Long id,
        String title,
        String content,
        AlertType alertType,
        LocalDateTime createdAt,
        List<AlertImageResponse> images,
        Long sendMemberId,
        Long sourceId
) {
    public static GetAlertResponse from(AlertView view) {
        return new GetAlertResponse(
                view.id(),
                view.title(),
                view.content(),
                view.alertType(),
                view.createdAt(),
                view.images().stream().map(i -> new AlertImageResponse(i.imageId(), i.imageUrl())).toList(),
                view.sendMemberId(),
                view.sourceId()
        );
    }
}
