package team.startup.gwangsan.application.notice.port.in;

import team.startup.gwangsan.application.notice.data.NoticeListCriteria;
import team.startup.gwangsan.application.notice.data.NoticeSummaryResult;

import java.util.List;

public interface NoticeListQuery {
    List<NoticeSummaryResult> query(NoticeListCriteria criteria);
}
