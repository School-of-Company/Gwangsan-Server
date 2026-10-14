package team.startup.gwangsan.application.notice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.notice.data.NoticeDetailCriteria;
import team.startup.gwangsan.application.notice.data.NoticeDetailResult;
import team.startup.gwangsan.application.notice.data.NoticeImageResult;
import team.startup.gwangsan.application.notice.data.NoticePlace;
import team.startup.gwangsan.application.notice.port.out.NoticeMemberPort;
import team.startup.gwangsan.application.notice.port.out.NoticePlacePort;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.exception.NoticeReaderNotFoundException;
import team.startup.gwangsan.domain.notice.model.Notice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeDetailService 단위 테스트")
class NoticeDetailServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 1, 9, 0);
    private static final Notice NOTICE = new Notice(5L, "제목", "내용", 3, 7L, CREATED_AT);
    private static final ActorContext AUTHOR = new ActorContext(7L, ActorContext.Role.ROLE_PLACE_ADMIN);
    private static final ActorContext HEAD_ADMIN = new ActorContext(8L, ActorContext.Role.ROLE_HEAD_ADMIN);

    @InjectMocks private NoticeDetailService service;

    @Mock private NoticePort noticePort;
    @Mock private NoticePlacePort noticePlacePort;
    @Mock private NoticeMemberPort noticeMemberPort;

    @Nested
    @DisplayName("query() 메서드는")
    class Describe_query {

        @Test
        @DisplayName("같은 지점 공지면 지점 이름·조회자 역할·이미지를 담아 돌려준다")
        void it_returns_detail_for_same_place() {
            when(noticePort.findById(5L)).thenReturn(Optional.of(NOTICE));
            when(noticeMemberPort.findPlaceIdOf(7L)).thenReturn(Optional.of(3));
            when(noticePlacePort.findPlace(3)).thenReturn(Optional.of(new NoticePlace(3, "광산구", 1)));
            NoticeImageResult image = new NoticeImageResult(40L, "http://img/40");
            when(noticePort.findImages(5L)).thenReturn(List.of(image));

            NoticeDetailResult result = service.query(new NoticeDetailCriteria(AUTHOR, 5L));

            assertThat(result).isEqualTo(new NoticeDetailResult(
                    5L, "제목", "내용", "광산구", CREATED_AT, "ROLE_PLACE_ADMIN", List.of(image), true));
        }

        @Test
        @DisplayName("다른 지점 공지면 NoticeNotFoundException 을 던진다")
        void it_hides_other_place_notice() {
            when(noticePort.findById(5L)).thenReturn(Optional.of(NOTICE));
            when(noticeMemberPort.findPlaceIdOf(7L)).thenReturn(Optional.of(4));
            when(noticePlacePort.findPlace(3)).thenReturn(Optional.of(new NoticePlace(3, "광산구", 1)));

            assertThatThrownBy(() -> service.query(new NoticeDetailCriteria(AUTHOR, 5L)))
                    .isInstanceOf(NoticeNotFoundException.class);
            verify(noticePort, never()).findImages(5L);
        }

        @Test
        @DisplayName("HEAD_ADMIN 은 같은 본부 산하 공지를 조회하며 isMe 는 작성자 기준이다")
        void it_returns_detail_for_head_admin_in_same_head() {
            when(noticePort.findById(5L)).thenReturn(Optional.of(NOTICE));
            when(noticeMemberPort.findPlaceIdOf(8L)).thenReturn(Optional.of(4));
            when(noticePlacePort.findPlace(3)).thenReturn(Optional.of(new NoticePlace(3, "광산구", 1)));
            when(noticePlacePort.findPlace(4)).thenReturn(Optional.of(new NoticePlace(4, "본부", 1)));
            when(noticePort.findImages(5L)).thenReturn(List.of());

            NoticeDetailResult result = service.query(new NoticeDetailCriteria(HEAD_ADMIN, 5L));

            assertThat(result.viewerRole()).isEqualTo("ROLE_HEAD_ADMIN");
            assertThat(result.isMe()).isFalse();
        }

        @Test
        @DisplayName("HEAD_ADMIN 이라도 다른 본부 공지면 NoticeNotFoundException 을 던진다")
        void it_hides_other_head_notice() {
            when(noticePort.findById(5L)).thenReturn(Optional.of(NOTICE));
            when(noticeMemberPort.findPlaceIdOf(8L)).thenReturn(Optional.of(4));
            when(noticePlacePort.findPlace(3)).thenReturn(Optional.of(new NoticePlace(3, "광산구", 1)));
            when(noticePlacePort.findPlace(4)).thenReturn(Optional.of(new NoticePlace(4, "다른 본부", 2)));

            assertThatThrownBy(() -> service.query(new NoticeDetailCriteria(HEAD_ADMIN, 5L)))
                    .isInstanceOf(NoticeNotFoundException.class);
        }

        @Test
        @DisplayName("공지가 없으면 조회자 확인 전에 NoticeNotFoundException 을 던진다")
        void it_throws_notice_not_found_exception_first() {
            when(noticePort.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.query(new NoticeDetailCriteria(AUTHOR, 5L)))
                    .isInstanceOf(NoticeNotFoundException.class);
            verifyNoInteractions(noticeMemberPort);
        }

        @Test
        @DisplayName("조회자의 회원 상세가 없으면 NoticeReaderNotFoundException 을 던진다")
        void it_throws_notice_reader_not_found_exception() {
            when(noticePort.findById(5L)).thenReturn(Optional.of(NOTICE));
            when(noticeMemberPort.findPlaceIdOf(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.query(new NoticeDetailCriteria(AUTHOR, 5L)))
                    .isInstanceOf(NoticeReaderNotFoundException.class);
        }
    }
}
