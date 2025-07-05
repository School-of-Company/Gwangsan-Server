package team.startup.gwangsan.domain.notice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.exception.NoticeForbiddenException;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.presentation.dto.reqeust.UpdateNoticeRequest;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.notice.service.UpdateNoticeService;
import team.startup.gwangsan.global.util.MemberUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UpdateNoticeServiceImpl implements UpdateNoticeService {

    private final NoticeRepository noticeRepository;
    private final ImageRepository imageRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public void execute(Long noticeId, UpdateNoticeRequest request, HttpServletRequest httpRequest) {
        Member member = memberUtil.getCurrentMember();

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFoundException::new);

        if (!notice.getMember().equals(member) &&
                member.getRole() != MemberRole.ROLE_HEAD_ADMIN &&
                member.getRole() != MemberRole.ROLE_PLACE_ADMIN) {
            throw new NoticeForbiddenException();
        }

        notice.update(request.title(), request.content());

        noticeImageRepository.deleteAll(notice.getNoticeImages());
        notice.getNoticeImages().clear();

        List<Image> images = imageRepository.findAllById(request.imageIds());

        if (images.size() != request.imageIds().size()) {
            throw new ImageNotFoundException();
        }

        images.forEach(notice::addImage);
    }
}