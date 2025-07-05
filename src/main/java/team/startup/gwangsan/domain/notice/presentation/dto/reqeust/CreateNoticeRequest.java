package team.startup.gwangsan.domain.notice.presentation.dto.reqeust;

import java.util.List;

public record CreateNoticeRequest(
        String title,
        String content,
        String placeName,
        List<Long> imageIds
) {}