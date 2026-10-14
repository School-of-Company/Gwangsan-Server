package team.startup.gwangsan.application.notice.port.in;

import team.startup.gwangsan.application.notice.data.NoticeImageLink;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 다른 업무(alert 등)가 쓰는 공지 공개 조회. */
public interface NoticeQuery {

    /** @throws team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException 공지가 없으면 */
    String getTitle(Long noticeId);

    /** 공지 ID 별 이미지 연결. 이미지가 없는 공지는 결과에 없다. 빈 입력이면 조회하지 않는다. */
    Map<Long, List<NoticeImageLink>> findImageLinks(Collection<Long> noticeIds);
}
