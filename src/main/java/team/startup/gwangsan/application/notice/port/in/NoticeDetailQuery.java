package team.startup.gwangsan.application.notice.port.in;

import team.startup.gwangsan.application.notice.data.NoticeDetailCriteria;
import team.startup.gwangsan.application.notice.data.NoticeDetailResult;

public interface NoticeDetailQuery {
    NoticeDetailResult query(NoticeDetailCriteria criteria);
}
