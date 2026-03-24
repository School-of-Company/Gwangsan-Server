package team.startup.gwangsan.domain.notice.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateNoticeRequest(
        @NotNull String title,
        @NotNull String content,
        @NotNull Integer placeId,
        List<Long> imageIds
) {}