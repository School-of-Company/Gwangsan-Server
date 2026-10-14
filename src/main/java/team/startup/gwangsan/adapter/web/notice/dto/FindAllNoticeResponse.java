package team.startup.gwangsan.adapter.web.notice.dto;

import team.startup.gwangsan.adapter.web.image.dto.GetImageResponse;

import java.util.List;

public record FindAllNoticeResponse(
        Long id,
        String title,
        String content,
        List<GetImageResponse> images,
        boolean isMe
) {}
