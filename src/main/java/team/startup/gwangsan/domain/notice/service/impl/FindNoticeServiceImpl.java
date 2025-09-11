package team.startup.gwangsan.domain.notice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.presentation.dto.response.FindNoticeResponse;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.notice.service.FindNoticeService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindNoticeServiceImpl implements FindNoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final MemberUtil memberUtil;

    @Override
    public FindNoticeResponse execute(Long noticeId) {
        Member member = memberUtil.getCurrentMember();

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFoundException::new);

        MemberDetail memberDetail = memberDetailRepository.findByMember(member)
                .orElseThrow(NotFoundMemberDetailException::new);

        if (member.getRole() == MemberRole.ROLE_HEAD_ADMIN) {
            if (!memberDetail.getPlace().getHead().equals(notice.getPlace().getHead())) {
                throw new NoticeNotFoundException();
            }
        } else {
            if (!memberDetail.getPlace().equals(notice.getPlace())) {
                throw new NoticeNotFoundException();
            }
        }

        List<GetImageResponse> imageResponses = noticeImageRepository.findAllByNotice(notice).stream()
                .map(NoticeImage::getImage)
                .map(image -> new GetImageResponse(image.getId(), image.getImageUrl()))
                .collect(Collectors.toList());

        return new FindNoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getPlace().getName(),
                notice.getCreatedAt(),
                member.getRole().name(),
                imageResponses
        );
    }
}