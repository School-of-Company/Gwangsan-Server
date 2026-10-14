package team.startup.gwangsan.application.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.notice.data.NoticeDetailCriteria;
import team.startup.gwangsan.application.notice.data.NoticeDetailResult;
import team.startup.gwangsan.application.notice.data.NoticePlace;
import team.startup.gwangsan.application.notice.port.in.NoticeDetailQuery;
import team.startup.gwangsan.application.notice.port.out.NoticeMemberPort;
import team.startup.gwangsan.application.notice.port.out.NoticePlacePort;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.exception.NoticePlaceNotFoundException;
import team.startup.gwangsan.domain.notice.exception.NoticeReaderNotFoundException;
import team.startup.gwangsan.domain.notice.model.Notice;
import team.startup.gwangsan.domain.notice.model.NoticeVisibility;

// 기존과 같이 트랜잭션을 열지 않는다(open-in-view 요청 범위에서 조회).
@Service
@RequiredArgsConstructor
public class NoticeDetailService implements NoticeDetailQuery {

    private final NoticePort noticePort;
    private final NoticePlacePort noticePlacePort;
    private final NoticeMemberPort noticeMemberPort;

    @Override
    public NoticeDetailResult query(NoticeDetailCriteria criteria) {
        ActorContext actor = criteria.actor();
        Notice notice = noticePort.findById(criteria.noticeId())
                .orElseThrow(NoticeNotFoundException::new);

        Integer readerPlaceId = noticeMemberPort.findPlaceIdOf(actor.memberId())
                .orElseThrow(NoticeReaderNotFoundException::new);

        NoticePlace noticePlace = place(notice.placeId());
        Integer readerHeadId = actor.role() == ActorContext.Role.ROLE_HEAD_ADMIN
                ? place(readerPlaceId).headId()
                : null;

        // 범위 밖 공지는 존재를 드러내지 않도록 404 로 응답한다(기존 계약).
        if (!NoticeVisibility.canRead(actor.role(), readerPlaceId, readerHeadId, notice.placeId(), noticePlace.headId())) {
            throw new NoticeNotFoundException();
        }

        return new NoticeDetailResult(
                notice.id(),
                notice.title(),
                notice.content(),
                noticePlace.name(),
                notice.createdAt(),
                actor.role().name(),
                noticePort.findImages(notice.id()),
                notice.isWrittenBy(actor.memberId())
        );
    }

    private NoticePlace place(Integer placeId) {
        return noticePlacePort.findPlace(placeId).orElseThrow(NoticePlaceNotFoundException::new);
    }
}
