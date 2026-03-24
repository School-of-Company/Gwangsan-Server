package team.startup.gwangsan.domain.notice.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateNoticeRequest(
        @NotNull String title,
        @NotNull String content,
        List<Long> imageIds
) {}
