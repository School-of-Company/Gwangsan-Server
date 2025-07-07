package team.startup.gwangsan.domain.notice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;
import team.startup.gwangsan.domain.notice.presentation.dto.reqeust.CreateNoticeRequest;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.notice.service.CreateNoticeService;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateNoticeServiceImpl implements CreateNoticeService {

    private final MemberUtil memberUtil;
    private final PlaceRepository placeRepository;
    private final NoticeRepository noticeRepository;
    private final ImageRepository imageRepository;
    private final NoticeImageRepository noticeImageRepository;

    @Override
    @Transactional
    public void execute(CreateNoticeRequest request) {
        Member admin = memberUtil.getCurrentMember();

        Place place = placeRepository.findByName(request.placeName())
                .orElseThrow(PlaceNotFoundException::new);

        Notice notice = Notice.builder()
                .title(request.title())
                .content(request.content())
                .place(place)
                .member(admin)
                .build();

        noticeRepository.save(notice);

        if (request.imageIds() != null && !request.imageIds().isEmpty()) {
            List<Image> images = imageRepository.findAllById(request.imageIds());

            if (images.size() != request.imageIds().size()) {
                throw new ImageNotFoundException();
            }

            List<NoticeImage> noticeImages = images.stream()
                    .map(image -> NoticeImage.builder()
                            .image(image)
                            .notice(notice)
                            .build())
                    .toList();

            noticeImageRepository.saveAll(noticeImages);
        }
    }
}