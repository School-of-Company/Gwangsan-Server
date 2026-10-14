package team.startup.gwangsan.application.notice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.notice.data.UpdateNoticeCommand;
import team.startup.gwangsan.application.notice.port.out.NoticeImagePort;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.notice.exception.NoticeImageNotFoundException;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.exception.UnauthorizedNoticeAccessException;
import team.startup.gwangsan.domain.notice.model.Notice;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateNoticeService 단위 테스트")
class UpdateNoticeServiceTest {

    private static final ActorContext AUTHOR = new ActorContext(7L, ActorContext.Role.ROLE_PLACE_ADMIN);
    private static final Notice NOTICE = new Notice(5L, "제목", "내용", 1, 7L, null);

    @InjectMocks private UpdateNoticeService service;

    @Mock private NoticePort noticePort;
    @Mock private NoticeImagePort noticeImagePort;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("작성자면 내용을 저장하고 이미지를 전체 교체한다")
        void it_updates_notice_and_replaces_images() {
            when(noticePort.findById(5L)).thenReturn(Optional.of(NOTICE));
            when(noticeImagePort.findExistingImageIds(List.of(8L))).thenReturn(List.of(8L));

            service.execute(new UpdateNoticeCommand(AUTHOR, 5L, "새 제목", "새 내용", List.of(8L)));

            InOrder order = inOrder(noticePort);
            order.verify(noticePort).save(new Notice(5L, "새 제목", "새 내용", 1, 7L, null));
            order.verify(noticePort).deleteImages(5L);
            order.verify(noticePort).addImages(5L, List.of(8L));
        }

        @Test
        @DisplayName("빈 이미지 목록이면 기존 이미지를 모두 지우고 빈 목록을 저장한다")
        void it_clears_all_images_when_image_ids_are_empty() {
            when(noticePort.findById(5L)).thenReturn(Optional.of(NOTICE));
            when(noticeImagePort.findExistingImageIds(List.of())).thenReturn(List.of());

            service.execute(new UpdateNoticeCommand(AUTHOR, 5L, "t", "c", List.of()));

            verify(noticePort).deleteImages(5L);
            verify(noticePort).addImages(5L, List.of());
        }

        @Test
        @DisplayName("공지가 없으면 NoticeNotFoundException 을 던진다")
        void it_throws_notice_not_found_exception() {
            when(noticePort.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new UpdateNoticeCommand(AUTHOR, 5L, "t", "c", List.of())))
                    .isInstanceOf(NoticeNotFoundException.class);
        }

        @Test
        @DisplayName("작성자가 아니면 UnauthorizedNoticeAccessException 을 던지고 저장하지 않는다")
        void it_throws_unauthorized_notice_access_exception() {
            when(noticePort.findById(5L)).thenReturn(Optional.of(NOTICE));

            assertThatThrownBy(() -> service.execute(new UpdateNoticeCommand(
                    new ActorContext(8L, ActorContext.Role.ROLE_HEAD_ADMIN), 5L, "t", "c", List.of())))
                    .isInstanceOf(UnauthorizedNoticeAccessException.class);
            verify(noticePort, never()).save(any());
            verifyNoInteractions(noticeImagePort);
        }

        @Test
        @DisplayName("이미지 개수가 맞지 않으면 NoticeImageNotFoundException 을 던진다 (트랜잭션 rollback 대상)")
        void it_throws_notice_image_not_found_exception() {
            when(noticePort.findById(5L)).thenReturn(Optional.of(NOTICE));
            when(noticeImagePort.findExistingImageIds(List.of(8L, 9L))).thenReturn(List.of(8L));

            assertThatThrownBy(() -> service.execute(new UpdateNoticeCommand(AUTHOR, 5L, "t", "c", List.of(8L, 9L))))
                    .isInstanceOf(NoticeImageNotFoundException.class);
            verify(noticePort, never()).addImages(any(), any());
        }
    }
}
