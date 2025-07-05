package team.startup.gwangsan.domain.notice.service;

import jakarta.servlet.http.HttpServletRequest;
import team.startup.gwangsan.domain.notice.presentation.dto.reqeust.CreateNoticeRequest;

public interface CreateNoticeService {
    void execute(CreateNoticeRequest request, HttpServletRequest httpRequest);
}