package team.startup.gwangsan.domain.notice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.presentation.dto.request.CreateNoticeRequest;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.global.event.CreateAlertMembersEvent;
import team.startup.gwangsan.global.event.SendNotificationEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateNoticeServiceImpl 단위 테스트")
class CreateNoticeServiceImplTest {

    @InjectMocks private CreateNoticeServiceImpl service;

    @Mock private MemberUtil memberUtil;
    @Mock private PlaceRepository placeRepository;
    @Mock private NoticeRepository noticeRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private NoticeImageRepository noticeImageRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private MemberDetailRepository memberDetailRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("이미지가 있는 정상 요청일 때")
        class Context_with_valid_request_with_images {

            @Test
            @DisplayName("공지를 저장하고 이미지를 저장하고 이벤트를 2회 발행한다")
            void it_saves_notice_and_images_and_publishes_events_twice() {
                Member admin = mock(Member.class);
                Place place = mock(Place.class);
                Image image = mock(Image.class);
                MemberDetail memberDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(placeRepository.findById(1)).thenReturn(Optional.of(place));
                when(noticeRepository.save(any())).thenReturn(mock(Notice.class));
                when(imageRepository.findAllById(List.of(10L))).thenReturn(List.of(image));
                when(memberDetailRepository.findAllByPlace(place)).thenReturn(List.of(memberDetail));
                when(memberDetail.getId()).thenReturn(1L);
                when(deviceTokenRepository.findByUserId(1L)).thenReturn(Optional.empty());

                CreateNoticeRequest request = new CreateNoticeRequest("제목", "내용", 1, List.of(10L));
                service.execute(request);

                verify(noticeRepository).save(any());
                verify(noticeImageRepository).saveAll(any());
                verify(applicationEventPublisher).publishEvent(any(SendNotificationEvent.class));
                verify(applicationEventPublisher).publishEvent(any(CreateAlertMembersEvent.class));
            }
        }

        @Nested
        @DisplayName("이미지가 없는 정상 요청일 때")
        class Context_with_valid_request_without_images {

            @Test
            @DisplayName("공지를 저장하고 이미지 저장은 호출하지 않는다")
            void it_saves_notice_without_saving_images() {
                Member admin = mock(Member.class);
                Place place = mock(Place.class);
                MemberDetail memberDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(placeRepository.findById(1)).thenReturn(Optional.of(place));
                when(noticeRepository.save(any())).thenReturn(mock(Notice.class));
                when(memberDetailRepository.findAllByPlace(place)).thenReturn(List.of(memberDetail));
                when(memberDetail.getId()).thenReturn(1L);
                when(deviceTokenRepository.findByUserId(1L)).thenReturn(Optional.empty());

                CreateNoticeRequest request = new CreateNoticeRequest("제목", "내용", 1, Collections.emptyList());
                service.execute(request);

                verify(noticeImageRepository, never()).saveAll(any());
            }
        }

        @Nested
        @DisplayName("placeId에 해당하는 Place가 없을 때")
        class Context_with_place_not_found {

            @Test
            @DisplayName("PlaceNotFoundException을 던진다")
            void it_throws_place_not_found_exception() {
                Member admin = mock(Member.class);
                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(placeRepository.findById(99)).thenReturn(Optional.empty());

                CreateNoticeRequest request = new CreateNoticeRequest("제목", "내용", 99, null);

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(PlaceNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("이미지 ID 개수와 실제 조회된 이미지 수가 불일치할 때")
        class Context_with_image_count_mismatch {

            @Test
            @DisplayName("ImageNotFoundException을 던진다")
            void it_throws_image_not_found_exception() {
                Member admin = mock(Member.class);
                Place place = mock(Place.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(placeRepository.findById(1)).thenReturn(Optional.of(place));
                when(noticeRepository.save(any())).thenReturn(mock(Notice.class));
                when(imageRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(mock(Image.class)));

                CreateNoticeRequest request = new CreateNoticeRequest("제목", "내용", 1, List.of(10L, 20L));

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(ImageNotFoundException.class);
            }
        }
    }
}