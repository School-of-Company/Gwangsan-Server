package team.startup.gwangsan.domain.notice.presentation.dto.reqeust;

import java.util.List;

public record UpdateNoticeRequest(
        String title,
        String content,
        List<Long> imageIds
) {}
