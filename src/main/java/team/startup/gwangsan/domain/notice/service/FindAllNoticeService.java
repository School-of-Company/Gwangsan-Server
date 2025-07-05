package team.startup.gwangsan.domain.notice.service;

import team.startup.gwangsan.domain.notice.presentation.dto.response.FindAllNoticeResponse;

import java.util.List;

public interface FindAllNoticeService {
    List<FindAllNoticeResponse> execute(int page, int size);
}
