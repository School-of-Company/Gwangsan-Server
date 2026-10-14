package team.startup.gwangsan.application.alert.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.alert.data.AlertImage;
import team.startup.gwangsan.application.alert.data.AlertView;
import team.startup.gwangsan.application.alert.port.out.LoadAlertImagesPort;
import team.startup.gwangsan.application.alert.port.out.LoadAlertPort;
import team.startup.gwangsan.domain.alert.model.Alert;
import team.startup.gwangsan.domain.alert.model.AlertType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertQueryService")
class AlertQueryServiceTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 1, 1, 10, 0);

    @Mock
    private LoadAlertPort loadAlertPort;
    @Mock
    private LoadAlertImagesPort images;
    @InjectMocks
    private AlertQueryService service;

    private static Alert alert(long id, AlertType type, Long sourceId, Long sender) {
        return new Alert(id, sourceId, sender, type, "제목" + id, "내용" + id, AT);
    }

    @Nested
    @DisplayName("query() 는")
    class Describe_query {

        @Test
        @DisplayName("알림이 없으면 빈 목록이고 공지 이미지 조회만 한다 (기존 조회 횟수)")
        void it_returns_empty() {
            when(loadAlertPort.loadReceived(1L)).thenReturn(List.of());
            when(images.noticeImages(List.of())).thenReturn(Map.of());

            assertThat(service.query(1L)).isEmpty();
            verify(images, never()).productImages(any());
            verify(images, never()).reportImages(any());
            verify(images, never()).productIdsByTradeCancel(any());
        }

        @Test
        @DisplayName("유형별 원천 이미지를 매핑하고 필드를 그대로 옮긴다")
        void it_maps_images_by_type() {
            when(loadAlertPort.loadReceived(1L)).thenReturn(List.of(
                    alert(1, AlertType.NOTICE, 100L, null),
                    alert(2, AlertType.TRADE_COMPLETE, 200L, 7L),
                    alert(3, AlertType.TRADE_CANCEL, 300L, 8L),
                    alert(4, AlertType.REPORT_REJECT, 400L, 9L),
                    alert(5, AlertType.REVIEW, 500L, 9L),
                    alert(6, AlertType.TRADE_CANCEL_REJECT, 301L, 9L)
            ));
            when(images.productIdsByTradeCancel(List.of(300L, 301L))).thenReturn(Map.of(300L, 201L));
            when(images.noticeImages(List.of(100L))).thenReturn(Map.of(100L, List.of(new AlertImage(11L, "n"))));
            when(images.productImages(any())).thenReturn(Map.of(
                    200L, List.of(new AlertImage(21L, "p200")),
                    201L, List.of(new AlertImage(22L, "p201"))));
            when(images.reportImages(List.of(400L))).thenReturn(Map.of(400L, List.of(new AlertImage(41L, "r"))));

            List<AlertView> views = service.query(1L);

            assertThat(views).extracting(AlertView::id).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
            assertThat(views.get(0).images()).containsExactly(new AlertImage(11L, "n"));
            assertThat(views.get(1).images()).containsExactly(new AlertImage(21L, "p200"));
            assertThat(views.get(2).images()).containsExactly(new AlertImage(22L, "p201"));
            assertThat(views.get(3).images()).containsExactly(new AlertImage(41L, "r"));
            assertThat(views.get(4).images()).isEmpty();
            assertThat(views.get(5).images()).as("상품을 찾지 못한 철회는 빈 목록").isEmpty();
            assertThat(views.get(1)).isEqualTo(new AlertView(2L, "제목2", "내용2", AlertType.TRADE_COMPLETE, AT,
                    List.of(new AlertImage(21L, "p200")), 7L, 200L));
            assertThat(views.get(0).sendMemberId()).isNull();
        }

        @Test
        @DisplayName("철회 대상 상품이 없으면 상품 이미지를 조회하지 않는다")
        void it_skips_product_query_when_trade_cancel_has_no_product() {
            when(loadAlertPort.loadReceived(1L)).thenReturn(List.of(alert(1, AlertType.TRADE_CANCEL, 300L, 2L)));
            when(images.productIdsByTradeCancel(List.of(300L))).thenReturn(Map.of());
            when(images.noticeImages(List.of())).thenReturn(Map.of());

            assertThat(service.query(1L).getFirst().images()).isEmpty();
            verify(images, never()).productImages(any());
        }
    }

    @Test
    @DisplayName("hasUnread() 는 미확인 receipt 존재 여부를 돌려준다")
    void it_checks_unread() {
        when(loadAlertPort.existsUnread(1L)).thenReturn(true);

        assertThat(service.hasUnread(1L)).isTrue();
    }
}
