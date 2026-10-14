package team.startup.gwangsan.application.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.notice.data.UpdateNoticeCommand;
import team.startup.gwangsan.application.notice.port.in.UpdateNoticeUseCase;
import team.startup.gwangsan.application.notice.port.out.NoticeImagePort;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.model.Notice;

@Service
@RequiredArgsConstructor
public class UpdateNoticeService implements UpdateNoticeUseCase {

    private final NoticePort noticePort;
    private final NoticeImagePort noticeImagePort;

    @Override
    @Transactional
    public void execute(UpdateNoticeCommand command) {
        Notice notice = noticePort.findById(command.noticeId())
                .orElseThrow(NoticeNotFoundException::new);

        noticePort.save(notice.updateBy(command.actor(), command.title(), command.content()));

        // 이미지는 전체 교체한다. 존재하지 않는 이미지가 있으면 내용 변경까지 rollback 된다.
        noticePort.deleteImages(notice.id());
        noticePort.addImages(notice.id(), NoticeImages.existing(noticeImagePort, command.imageIds()));
    }
}
