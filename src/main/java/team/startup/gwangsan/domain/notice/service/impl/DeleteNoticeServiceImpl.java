package team.startup.gwangsan.domain.notice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.exception.NoticeForbiddenException;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.notice.service.DeleteNoticeService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class DeleteNoticeServiceImpl implements DeleteNoticeService {

    private final NoticeRepository noticeRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public void execute(Long noticeId) {
        Member member = memberUtil.getCurrentMember();

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFoundException::new);

        if (!notice.getMember().equals(member) &&
                member.getRole() != MemberRole.ROLE_HEAD_ADMIN &&
                member.getRole() != MemberRole.ROLE_PLACE_ADMIN) {
            throw new NoticeForbiddenException();
        }

        noticeRepository.delete(notice);
    }
}

