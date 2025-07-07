package team.startup.gwangsan.domain.notice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;
import team.startup.gwangsan.domain.notice.presentation.dto.response.FindAllNoticeResponse;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.notice.service.FindAllNoticeService;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindAllNoticeServiceImpl implements FindAllNoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final MemberUtil memberUtil;
    private final PlaceRepository placeRepository;

    @Override
    public List<FindAllNoticeResponse> execute(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Member member = memberUtil.getCurrentMember();
        Page<Notice> notices;

        MemberDetail memberDetail = memberDetailRepository.findByMember(member)
                .orElseThrow(NotFoundMemberDetailException::new);

        Place myPlace = memberDetail.getPlace();

        if (member.getRole() == MemberRole.ROLE_HEAD_ADMIN) {
            Head myHead = myPlace.getHead();

            List<Place> branchPlaces = placeRepository.findAllByHead(myHead);

            notices = noticeRepository.findAllByPlaceIn(branchPlaces, pageable);
        } else {
            notices = noticeRepository.findAllByPlace(myPlace, pageable);
        }

        return notices.getContent().stream()
                .map(notice -> {
                    List<NoticeImage> noticeImages = noticeImageRepository.findAllByNotice(notice);

                    List<GetImageResponse> imageResponses = noticeImages.stream()
                            .map(ni -> new GetImageResponse(
                                    ni.getImage().getId(),
                                    ni.getImage().getImageUrl()
                            ))
                            .collect(Collectors.toList());

                    return new FindAllNoticeResponse(
                            notice.getId(),
                            notice.getTitle(),
                            notice.getContent(),
                            notice.getPlace().getName(),
                            notice.getCreatedAt(),
                            notice.getMember().getRole().name(),
                            imageResponses
                    );
                })
                .collect(Collectors.toList());
    }
}
