package team.startup.gwangsan.domain.notice.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.presentation.dto.reqeust.CreateNoticeRequest;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.notice.service.CreateNoticeService;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.global.event.NoticeCreatedEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateNoticeServiceImpl implements CreateNoticeService {

    private final MemberUtil memberUtil;
    private final PlaceRepository placeRepository;
    private final NoticeRepository noticeRepository;
    private final ImageRepository imageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberDetailRepository memberDetailRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    @Transactional
    public void execute(CreateNoticeRequest request, HttpServletRequest httpRequest) {
        Member admin = memberUtil.getCurrentMember();

        if (admin.getRole() != MemberRole.ROLE_HEAD_ADMIN && admin.getRole() != MemberRole.ROLE_PLACE_ADMIN) {
            throw new ForbiddenException();
        }

        Place place = null;
        if (request.placeName() != null) {
            place = placeRepository.findByName(request.placeName())
                    .orElseThrow(PlaceNotFoundException::new);
        }

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

            for (Image image : images) {
                notice.addImage(image);
            }
        }

        List<String> deviceTokens = getDeviceTokens(notice);

        eventPublisher.publishEvent(
                new NoticeCreatedEvent(
                        notice,
                        deviceTokens,
                        NotificationType.NOTICE
                )
        );
    }

    private List<String> getDeviceTokens(Notice notice) {
        List<MemberDetail> targets;

        if (notice.getPlace() == null) {
            targets = memberDetailRepository.findAll();
        } else {
            targets = memberDetailRepository.findAllByPlace(notice.getPlace());
        }

        return targets.stream()
                .map(md -> md.getMember().getId().toString())
                .map(deviceTokenRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(deviceToken -> deviceToken.getDeviceToken())
                .filter(Objects::nonNull)
                .toList();
    }
}