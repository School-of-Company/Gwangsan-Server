package team.startup.gwangsan.application.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.notice.port.in.ReassignNoticeAuthorUseCase;
import team.startup.gwangsan.application.notice.port.out.NoticePort;

@Service
@RequiredArgsConstructor
public class ReassignNoticeAuthorService implements ReassignNoticeAuthorUseCase {

    private final NoticePort noticePort;

    @Override
    @Transactional
    public void reassign(Long memberId, Long dummyMemberId) {
        noticePort.reassignAuthor(memberId, dummyMemberId);
    }
}
