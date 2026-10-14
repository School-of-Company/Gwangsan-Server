package team.startup.gwangsan.application.notice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.notice.data.NoticeImageLink;
import team.startup.gwangsan.application.notice.data.NoticeImageResult;
import team.startup.gwangsan.application.notice.data.NoticeListCriteria;
import team.startup.gwangsan.application.notice.data.NoticeSummaryResult;
import team.startup.gwangsan.application.notice.port.out.NoticeMemberPort;
import team.startup.gwangsan.application.notice.port.out.NoticePlacePort;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.notice.exception.NoticeReaderNotFoundException;
import team.startup.gwangsan.domain.notice.model.Notice;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeListService 단위 테스트")
class NoticeListServiceTest {

    private static final ActorContext PLACE_ADMIN = new ActorContext(7L, ActorContext.Role.ROLE_PLACE_ADMIN);
    private static final ActorContext HEAD_ADMIN = new ActorContext(7L, ActorContext.Role.ROLE_HEAD_ADMIN);

    @InjectMocks private NoticeListService service;

    @Mock private NoticePort noticePort;
    @Mock private NoticePlacePort noticePlacePort;
    @Mock private NoticeMemberPort noticeMemberPort;

    @Nested
    @DisplayName("query() 메서드는")
    class Describe_query {

        @Test
        @DisplayName("HEAD_ADMIN 이 아니면 자기 지점 공지를 cursor 로 조회한다")
        void it_reads_own_place_with_cursor() {
            when(noticeMemberPort.findPlaceIdOf(7L)).thenReturn(Optional.of(3));
            when(noticePort.findPage(List.of(3), 30L, 2)).thenReturn(List.of());
            when(noticePort.findImageLinks(List.of())).thenReturn(Map.of());

            assertThat(service.query(new NoticeListCriteria(PLACE_ADMIN, 30L, 2))).isEmpty();
            verifyNoInteractions(noticePlacePort);
        }

        @Test
        @DisplayName("HEAD_ADMIN 이면 같은 본부 산하 지점 전체를 첫 페이지부터 조회한다")
        void it_reads_same_head_places_for_head_admin() {
            when(noticeMemberPort.findPlaceIdOf(7L)).thenReturn(Optional.of(3));
            when(noticePlacePort.findPlaceIdsInSameHead(3)).thenReturn(List.of(3, 4));
            when(noticePort.findPage(List.of(3, 4), null, 10)).thenReturn(List.of());
            when(noticePort.findImageLinks(List.of())).thenReturn(Map.of());

            service.query(new NoticeListCriteria(HEAD_ADMIN, null, 10));

            verify(noticePort).findPage(List.of(3, 4), null, 10);
        }

        @Test
        @DisplayName("공지별 이미지와 본인 작성 여부를 붙여 순서대로 돌려준다")
        void it_returns_summaries_with_images_and_is_me() {
            when(noticeMemberPort.findPlaceIdOf(7L)).thenReturn(Optional.of(3));
            when(noticePort.findPage(List.of(3), null, 10)).thenReturn(List.of(
                    new Notice(2L, "둘", "c2", 3, 7L, null),
                    new Notice(1L, "하나", "c1", 3, null, null)));
            // 공지 목록 응답은 연결 행 ID 가 아니라 이미지 ID 를 쓴다(기존 계약).
            when(noticePort.findImageLinks(List.of(2L, 1L)))
                    .thenReturn(Map.of(2L, List.of(new NoticeImageLink(99L, 40L, "http://img/40"))));
            NoticeImageResult image = new NoticeImageResult(40L, "http://img/40");

            List<NoticeSummaryResult> result = service.query(new NoticeListCriteria(PLACE_ADMIN, null, 10));

            assertThat(result).containsExactly(
                    new NoticeSummaryResult(2L, "둘", "c2", List.of(image), true),
                    new NoticeSummaryResult(1L, "하나", "c1", List.of(), false));
        }

        @Test
        @DisplayName("조회자의 회원 상세가 없으면 NoticeReaderNotFoundException 을 던진다")
        void it_throws_notice_reader_not_found_exception() {
            when(noticeMemberPort.findPlaceIdOf(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.query(new NoticeListCriteria(PLACE_ADMIN, null, 10)))
                    .isInstanceOf(NoticeReaderNotFoundException.class);
            verifyNoInteractions(noticePort);
        }
    }
}
