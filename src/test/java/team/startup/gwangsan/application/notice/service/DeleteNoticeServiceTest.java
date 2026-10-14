package team.startup.gwangsan.application.notice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.notice.data.DeleteNoticeCommand;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.notice.exception.NoticeForbiddenException;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.model.Notice;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteNoticeService 단위 테스트")
class DeleteNoticeServiceTest {

    private static final Notice NOTICE = new Notice(5L, "제목", "내용", 1, 7L, null);

    @InjectMocks private DeleteNoticeService service;

    @Mock private NoticePort noticePort;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("작성자 본인이면 공지를 삭제한다")
        void it_deletes_notice() {
            when(noticePort.findById(5L)).thenReturn(Optional.of(NOTICE));

            service.execute(new DeleteNoticeCommand(new ActorContext(7L, ActorContext.Role.ROLE_PLACE_ADMIN), 5L));

            verify(noticePort).delete(5L);
        }

        @Test
        @DisplayName("HEAD_ADMIN 은 타인의 공지도 삭제한다")
        void it_deletes_notice_by_head_admin() {
            when(noticePort.findById(5L)).thenReturn(Optional.of(NOTICE));

            service.execute(new DeleteNoticeCommand(new ActorContext(9L, ActorContext.Role.ROLE_HEAD_ADMIN), 5L));

            verify(noticePort).delete(5L);
        }

        @Test
        @DisplayName("공지가 없으면 NoticeNotFoundException 을 던진다")
        void it_throws_notice_not_found_exception() {
            when(noticePort.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new DeleteNoticeCommand(new ActorContext(7L, ActorContext.Role.ROLE_PLACE_ADMIN), 5L)))
                    .isInstanceOf(NoticeNotFoundException.class);
        }

        @Test
        @DisplayName("본인 글도 아니고 HEAD_ADMIN 도 아니면 NoticeForbiddenException 을 던진다")
        void it_throws_notice_forbidden_exception() {
            when(noticePort.findById(5L)).thenReturn(Optional.of(NOTICE));

            assertThatThrownBy(() -> service.execute(new DeleteNoticeCommand(new ActorContext(9L, ActorContext.Role.ROLE_PLACE_ADMIN), 5L)))
                    .isInstanceOf(NoticeForbiddenException.class);
            verify(noticePort, never()).delete(any());
        }
    }
}
