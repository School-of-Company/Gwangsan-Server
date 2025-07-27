package team.startup.gwangsan.domain.notice.service;

import team.startup.gwangsan.domain.notice.presentation.dto.request.CreateNoticeRequest;

public interface CreateNoticeService {
    void execute(CreateNoticeRequest request);
}