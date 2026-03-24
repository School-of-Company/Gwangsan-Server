package team.startup.gwangsan.domain.notice.service;

import jakarta.servlet.http.HttpServletRequest;
import team.startup.gwangsan.domain.notice.presentation.dto.response.FindNoticeResponse;

public interface FindNoticeService {
    FindNoticeResponse execute(Long noticeId);
}
