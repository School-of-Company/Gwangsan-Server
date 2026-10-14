package team.startup.gwangsan.application.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.notice.data.NoticeImageLink;
import team.startup.gwangsan.application.notice.data.NoticeImageResult;
import team.startup.gwangsan.application.notice.data.NoticeListCriteria;
import team.startup.gwangsan.application.notice.data.NoticeSummaryResult;
import team.startup.gwangsan.application.notice.port.in.NoticeListQuery;
import team.startup.gwangsan.application.notice.port.out.NoticeMemberPort;
import team.startup.gwangsan.application.notice.port.out.NoticePlacePort;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.notice.exception.NoticeReaderNotFoundException;
import team.startup.gwangsan.domain.notice.model.Notice;

import java.util.List;
import java.util.Map;

// 기존과 같이 트랜잭션을 열지 않는다(open-in-view 요청 범위에서 조회).
@Service
@RequiredArgsConstructor
public class NoticeListService implements NoticeListQuery {

    private final NoticePort noticePort;
    private final NoticePlacePort noticePlacePort;
    private final NoticeMemberPort noticeMemberPort;

    @Override
    public List<NoticeSummaryResult> query(NoticeListCriteria criteria) {
        ActorContext actor = criteria.actor();
        Integer readerPlaceId = noticeMemberPort.findPlaceIdOf(actor.memberId())
                .orElseThrow(NoticeReaderNotFoundException::new);

        List<Integer> placeIds = actor.role() == ActorContext.Role.ROLE_HEAD_ADMIN
                ? noticePlacePort.findPlaceIdsInSameHead(readerPlaceId)
                : List.of(readerPlaceId);

        List<Notice> notices = noticePort.findPage(placeIds, criteria.lastId(), criteria.size());
        Map<Long, List<NoticeImageLink>> images = noticePort.findImageLinks(
                notices.stream().map(Notice::id).toList());

        return notices.stream()
                .map(notice -> new NoticeSummaryResult(
                        notice.id(),
                        notice.title(),
                        notice.content(),
                        images.getOrDefault(notice.id(), List.of()).stream()
                                .map(link -> new NoticeImageResult(link.imageId(), link.imageUrl()))
                                .toList(),
                        notice.isWrittenBy(actor.memberId())))
                .toList();
    }
}
