package team.startup.gwangsan.domain.notice.service;

import team.startup.gwangsan.domain.notice.presentation.dto.reqeust.CreateNoticeRequest;

public interface CreateNoticeService {
    void execute(CreateNoticeRequest request);
}