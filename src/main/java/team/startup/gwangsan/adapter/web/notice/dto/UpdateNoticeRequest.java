package team.startup.gwangsan.adapter.web.notice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateNoticeRequest(
        @NotNull String title,
        @NotNull String content,
        List<Long> imageIds
) {}
