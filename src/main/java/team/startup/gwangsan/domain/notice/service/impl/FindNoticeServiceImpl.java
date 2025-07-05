package team.startup.gwangsan.domain.notice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.presentation.dto.response.FindNoticeResponse;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.notice.service.FindNoticeService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindNoticeServiceImpl implements FindNoticeService {

    private final NoticeRepository noticeRepository;
    private final MemberUtil memberUtil;

    @Override
    public FindNoticeResponse execute(Long noticeId) {
        Member member = memberUtil.getCurrentMember();

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFoundException::new);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<GetImageResponse> imageResponses = notice.getNoticeImages().stream()
                .map(NoticeImage::getImage)
                .map(image -> new GetImageResponse(image.getId(), image.getImageUrl()))
                .collect(Collectors.toList());

        return new FindNoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getPlace().getName(),
                notice.getCreatedAt().format(formatter),
                member.getRole().name(),
                imageResponses
        );
    }
}
