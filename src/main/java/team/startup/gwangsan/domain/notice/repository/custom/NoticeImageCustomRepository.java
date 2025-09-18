package team.startup.gwangsan.domain.notice.repository.custom;

import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;

import java.util.List;

public interface NoticeImageCustomRepository {
    List<NoticeImage> findAllByNotice(Notice notice);

    List<NoticeImage> findAllByNoticeIdIn(List<Long> noticeIds);
}
