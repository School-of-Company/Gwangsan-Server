package team.startup.gwangsan.domain.notice.service;

import team.startup.gwangsan.domain.notice.presentation.dto.request.UpdateNoticeRequest;

public interface UpdateNoticeService {
    void execute(Long noticeId, UpdateNoticeRequest request);
}
