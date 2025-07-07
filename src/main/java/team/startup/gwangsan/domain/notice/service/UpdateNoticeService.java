package team.startup.gwangsan.domain.notice.service;

import team.startup.gwangsan.domain.notice.presentation.dto.reqeust.UpdateNoticeRequest;

public interface UpdateNoticeService {
    void execute(Long noticeId, UpdateNoticeRequest request);
}
