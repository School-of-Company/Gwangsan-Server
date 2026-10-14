package team.startup.gwangsan.application.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.notice.data.DeleteNoticeCommand;
import team.startup.gwangsan.application.notice.port.in.DeleteNoticeUseCase;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.model.Notice;

@Service
@RequiredArgsConstructor
public class DeleteNoticeService implements DeleteNoticeUseCase {

    private final NoticePort noticePort;

    @Override
    @Transactional
    public void execute(DeleteNoticeCommand command) {
        Notice notice = noticePort.findById(command.noticeId())
                .orElseThrow(NoticeNotFoundException::new);

        notice.checkDeletableBy(command.actor());

        noticePort.delete(notice.id());
    }
}
