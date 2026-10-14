package team.startup.gwangsan.application.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.notice.data.CreateNoticeCommand;
import team.startup.gwangsan.application.notice.port.in.CreateNoticeUseCase;
import team.startup.gwangsan.application.notice.port.out.NoticeImagePort;
import team.startup.gwangsan.application.notice.port.out.NoticeMemberPort;
import team.startup.gwangsan.application.notice.port.out.NoticeNotificationPort;
import team.startup.gwangsan.application.notice.port.out.NoticePlacePort;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.notice.exception.NoticePlaceNotFoundException;
import team.startup.gwangsan.domain.notice.model.Notice;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateNoticeService implements CreateNoticeUseCase {

    private final NoticePort noticePort;
    private final NoticePlacePort noticePlacePort;
    private final NoticeImagePort noticeImagePort;
    private final NoticeMemberPort noticeMemberPort;
    private final NoticeNotificationPort noticeNotificationPort;

    @Override
    @Transactional
    public void execute(CreateNoticeCommand command) {
        noticePlacePort.findPlace(command.placeId())
                .orElseThrow(NoticePlaceNotFoundException::new);

        Notice notice = noticePort.save(Notice.create(
                command.title(), command.content(), command.placeId(), command.actor().memberId()));

        List<Long> imageIds = command.imageIds();
        if (imageIds != null && !imageIds.isEmpty()) {
            noticePort.addImages(notice.id(), NoticeImages.existing(noticeImagePort, imageIds));
        }

        List<Long> memberIds = noticeMemberPort.findMemberIdsInPlace(command.placeId());
        noticeNotificationPort.notifyCreated(notice.id(), memberIds);
    }
}
