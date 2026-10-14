package team.startup.gwangsan.application.notice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.notice.data.CreateNoticeCommand;
import team.startup.gwangsan.application.notice.data.NoticePlace;
import team.startup.gwangsan.application.notice.port.out.NoticeImagePort;
import team.startup.gwangsan.application.notice.port.out.NoticeMemberPort;
import team.startup.gwangsan.application.notice.port.out.NoticeNotificationPort;
import team.startup.gwangsan.application.notice.port.out.NoticePlacePort;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.notice.exception.NoticeImageNotFoundException;
import team.startup.gwangsan.domain.notice.exception.NoticePlaceNotFoundException;
import team.startup.gwangsan.domain.notice.model.Notice;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateNoticeService 단위 테스트")
class CreateNoticeServiceTest {

    private static final ActorContext ADMIN = new ActorContext(7L, ActorContext.Role.ROLE_PLACE_ADMIN);
    private static final Notice SAVED = new Notice(100L, "제목", "내용", 1, 7L, null);

    @InjectMocks private CreateNoticeService service;

    @Mock private NoticePort noticePort;
    @Mock private NoticePlacePort noticePlacePort;
    @Mock private NoticeImagePort noticeImagePort;
    @Mock private NoticeMemberPort noticeMemberPort;
    @Mock private NoticeNotificationPort noticeNotificationPort;

    private void placeExists() {
        when(noticePlacePort.findPlace(1)).thenReturn(Optional.of(new NoticePlace(1, "지점", 1)));
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("이미지가 있으면 공지·이미지를 저장하고 지점 회원에게 알림을 요청한다")
        void it_saves_notice_and_images_and_notifies_place_members() {
            placeExists();
            when(noticePort.save(any())).thenReturn(SAVED);
            when(noticeImagePort.findExistingImageIds(List.of(11L, 10L))).thenReturn(List.of(10L, 11L));
            when(noticeMemberPort.findMemberIdsInPlace(1)).thenReturn(List.of(1L, 2L));

            service.execute(new CreateNoticeCommand(ADMIN, "제목", "내용", 1, List.of(11L, 10L)));

            InOrder order = inOrder(noticePort, noticeNotificationPort);
            order.verify(noticePort).save(Notice.create("제목", "내용", 1, 7L));
            // 저장 순서는 이미지 조회 결과 순서를 따른다.
            order.verify(noticePort).addImages(100L, List.of(10L, 11L));
            order.verify(noticeNotificationPort).notifyCreated(100L, List.of(1L, 2L));
        }

        @Test
        @DisplayName("이미지 id 가 null 이거나 비어 있으면 이미지 조회와 저장을 생략한다")
        void it_skips_images_when_image_ids_are_null_or_empty() {
            placeExists();
            when(noticePort.save(any())).thenReturn(SAVED);
            when(noticeMemberPort.findMemberIdsInPlace(1)).thenReturn(List.of());

            service.execute(new CreateNoticeCommand(ADMIN, "제목", "내용", 1, null));
            service.execute(new CreateNoticeCommand(ADMIN, "제목", "내용", 1, List.of()));

            verifyNoInteractions(noticeImagePort);
            verify(noticePort, never()).addImages(any(), any());
            verify(noticeNotificationPort, times(2)).notifyCreated(100L, List.of());
        }

        @Test
        @DisplayName("지점이 없으면 NoticePlaceNotFoundException 을 던지고 저장하지 않는다")
        void it_throws_notice_place_not_found_exception() {
            when(noticePlacePort.findPlace(1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new CreateNoticeCommand(ADMIN, "제목", "내용", 1, null)))
                    .isInstanceOf(NoticePlaceNotFoundException.class);
            verifyNoInteractions(noticePort, noticeNotificationPort);
        }

        @Test
        @DisplayName("이미지 개수가 맞지 않으면(중복 포함) NoticeImageNotFoundException 을 던지고 알림하지 않는다")
        void it_throws_notice_image_not_found_exception() {
            placeExists();
            when(noticePort.save(any())).thenReturn(SAVED);
            when(noticeImagePort.findExistingImageIds(List.of(10L, 10L))).thenReturn(List.of(10L));

            assertThatThrownBy(() -> service.execute(new CreateNoticeCommand(ADMIN, "제목", "내용", 1, List.of(10L, 10L))))
                    .isInstanceOf(NoticeImageNotFoundException.class);
            verify(noticePort, never()).addImages(any(), any());
            verifyNoInteractions(noticeNotificationPort);
        }
    }
}
