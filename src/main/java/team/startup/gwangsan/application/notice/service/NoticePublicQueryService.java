package team.startup.gwangsan.application.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.notice.data.NoticeImageLink;
import team.startup.gwangsan.application.notice.port.in.NoticeQuery;
import team.startup.gwangsan.application.notice.port.in.UnlinkNoticeImageUseCase;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.model.Notice;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NoticePublicQueryService implements NoticeQuery, UnlinkNoticeImageUseCase {

    private final NoticePort noticePort;

    @Override
    @Transactional(readOnly = true)
    public String getTitle(Long noticeId) {
        return noticePort.findById(noticeId).map(Notice::title).orElseThrow(NoticeNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<NoticeImageLink>> findImageLinks(Collection<Long> noticeIds) {
        if (noticeIds.isEmpty()) {
            return Map.of();
        }
        return noticePort.findImageLinks(List.copyOf(noticeIds));
    }

    @Override
    @Transactional
    public void unlink(Long noticeId, Long imageId) {
        noticePort.unlinkImage(noticeId, imageId);
    }
}
