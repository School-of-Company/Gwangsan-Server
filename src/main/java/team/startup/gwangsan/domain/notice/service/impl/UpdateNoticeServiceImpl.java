package team.startup.gwangsan.domain.notice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.presentation.dto.reqeust.UpdateNoticeRequest;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.notice.service.UpdateNoticeService;
import team.startup.gwangsan.global.util.MemberUtil;

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
    public void execute(Long noticeId, UpdateNoticeRequest request) {
        Member member = memberUtil.getCurrentMember();

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFoundException::new);

        notice.update(request.title(), request.content());

        List<NoticeImage> oldNoticeImages = noticeImageRepository.findAllByNotice(notice);
        noticeImageRepository.deleteAll(oldNoticeImages);

        List<Image> images = imageRepository.findAllById(request.imageIds());

        if (images.size() != request.imageIds().size()) {
            throw new ImageNotFoundException();
        }

        List<NoticeImage> newNoticeImages = images.stream()
                .map(image -> NoticeImage.builder()
                        .notice(notice)
                        .image(image)
                        .build())
                .toList();

        noticeImageRepository.saveAll(newNoticeImages);
    }
}
