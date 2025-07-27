package team.startup.gwangsan.domain.notice.service.impl;

import lombok.RequiredArgsConstructor;
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
import java.util.Map;
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
    public List<FindAllNoticeResponse> execute(Long lastId, int size) {
        Member member = memberUtil.getCurrentMember();
        MemberDetail memberDetail = memberDetailRepository.findByMember(member)
                .orElseThrow(NotFoundMemberDetailException::new);
        Place myPlace = memberDetail.getPlace();
        MemberRole myRole = member.getRole();
        Pageable pageable = PageRequest.of(0, size);

        List<Notice> notices;

        if (myRole == MemberRole.ROLE_HEAD_ADMIN) {
            Head myHead = myPlace.getHead();
            List<Place> branchPlaces = placeRepository.findByHead(myHead);

            notices = (lastId == null)
                    ? noticeRepository.findByPlaceInAndTargetRolesContainingOrderByIdDesc(branchPlaces, myRole, pageable)
                    : noticeRepository.findByPlaceInAndTargetRolesContainingAndIdLessThanOrderByIdDesc(branchPlaces, myRole, lastId, pageable);

        } else {
            notices = (lastId == null)
                    ? noticeRepository.findByPlaceAndTargetRolesContainingOrderByIdDesc(myPlace, myRole, pageable)
                    : noticeRepository.findByPlaceAndTargetRolesContainingAndIdLessThanOrderByIdDesc(myPlace, myRole, lastId, pageable);
        }

        List<Long> noticeIds = notices.stream().map(Notice::getId).toList();
        List<NoticeImage> allNoticeImages = noticeImageRepository.findAllByNoticeIdIn(noticeIds);

        Map<Long, List<NoticeImage>> noticeImageMap = allNoticeImages.stream()
                .collect(Collectors.groupingBy(ni -> ni.getNotice().getId()));

        return notices.stream()
                .map(notice -> {
                    List<GetImageResponse> imageResponses = noticeImageMap
                            .getOrDefault(notice.getId(), List.of())
                            .stream()
                            .map(ni -> new GetImageResponse(
                                    ni.getImage().getId(),
                                    ni.getImage().getImageUrl()
                            ))
                            .toList();

                    boolean isMe = notice.getMember().getId().equals(member.getId());

                    return new FindAllNoticeResponse(
                            notice.getId(),
                            notice.getTitle(),
                            notice.getContent(),
                            imageResponses,
                            isMe
                    );
                })
                .toList();
    }
}
