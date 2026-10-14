package team.startup.gwangsan.application.notice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.notice.data.NoticeImageLink;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.model.Notice;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticePublicQueryService 단위 테스트")
class NoticePublicQueryServiceTest {

    @InjectMocks private NoticePublicQueryService service;

    @Mock private NoticePort noticePort;

    @Nested
    @DisplayName("getTitle() 은")
    class Describe_getTitle {

        @Test
        @DisplayName("공지 제목을 돌려준다")
        void it_returns_title() {
            when(noticePort.findById(1L)).thenReturn(Optional.of(new Notice(1L, "제목", "내용", 3, 7L, null)));

            assertThat(service.getTitle(1L)).isEqualTo("제목");
        }

        @Test
        @DisplayName("공지가 없으면 NoticeNotFoundException 을 던진다")
        void it_throws_notice_not_found_exception() {
            when(noticePort.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTitle(1L)).isInstanceOf(NoticeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findImageLinks() 는")
    class Describe_findImageLinks {

        @Test
        @DisplayName("공지 id 로 연결을 조회한다")
        void it_delegates_with_notice_ids() {
            Map<Long, List<NoticeImageLink>> links = Map.of(1L, List.of(new NoticeImageLink(11L, 900L, "https://n")));
            when(noticePort.findImageLinks(List.of(1L))).thenReturn(links);

            assertThat(service.findImageLinks(Set.of(1L))).isEqualTo(links);
        }

        @Test
        @DisplayName("빈 입력이면 조회하지 않고 빈 Map 을 돌려준다")
        void it_skips_query_for_empty_input() {
            assertThat(service.findImageLinks(List.of())).isEmpty();
            verifyNoInteractions(noticePort);
        }
    }

    @Test
    @DisplayName("unlink() 는 공지-이미지 연결만 지운다")
    void it_unlinks_notice_image() {
        service.unlink(1L, 2L);

        verify(noticePort).unlinkImage(1L, 2L);
    }
}
