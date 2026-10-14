package team.startup.gwangsan.adapter.persistence.notice;

import java.util.List;

public interface NoticeImageJpaCustomRepository {
    List<NoticeImageJpaEntity> findAllByNoticeId(Long noticeId);

    List<NoticeImageJpaEntity> findAllByNoticeIdIn(List<Long> noticeIds);
}
