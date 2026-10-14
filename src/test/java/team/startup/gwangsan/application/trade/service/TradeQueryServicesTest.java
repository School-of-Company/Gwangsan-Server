package team.startup.gwangsan.application.trade.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.trade.data.*;
import team.startup.gwangsan.application.trade.port.out.*;
import team.startup.gwangsan.domain.trade.exception.InvalidTradeStatisticsPeriodException;
import team.startup.gwangsan.domain.trade.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("거래 조회 유스케이스 단위 테스트")
class TradeQueryServicesTest {

    @Mock private TradeProductPort tradeProductPort;
    @Mock private TradeCompletePort tradeCompletePort;
    @Mock private TradeHistoryPort tradeHistoryPort;
    @Mock private TradeStatisticsPort tradeStatisticsPort;
    @Mock private TradePlacePort tradePlacePort;

    @Nested
    @DisplayName("TradeStateService 는")
    class Describe_state {

        private TradeStateService service() {
            return new TradeStateService(tradeProductPort, tradeCompletePort);
        }

        @Test
        @DisplayName("현재 상품 단계와 PENDING/COMPLETED 요청으로 스냅샷을 만든다")
        void it_builds_snapshot() {
            LocalDateTime at = LocalDateTime.of(2026, 1, 1, 9, 0);
            when(tradeProductPort.find(1L)).thenReturn(new TradeProduct(1L, 5L, true, ProductPhase.RESERVED, 100));
            when(tradeCompletePort.findRequestStates(1L, 2L, 5L))
                    .thenReturn(List.of(new TradeRequestState(TradeStatus.PENDING, true, at)));

            assertThat(service().query(new TradeStateKey(1L, 2L, 5L)))
                    .isEqualTo(new TradeStateSnapshot(false, true, true, at));
        }

        @Test
        @DisplayName("완료된 거래를 참여자·완료 시각으로 돌려준다")
        void it_finds_completed_trade() {
            LocalDateTime completedAt = LocalDateTime.of(2026, 1, 2, 9, 0);
            when(tradeCompletePort.findCompleted(1L)).thenReturn(Optional.of(
                    new TradeComplete(10L, 1L, 2L, 5L, TradeStatus.COMPLETED, true, completedAt.minusHours(1), completedAt)));

            assertThat(service().query(1L)).contains(new CompletedTrade(10L, 2L, 5L, completedAt));
        }
    }

    @Nested
    @DisplayName("TradeHistoryService 는")
    class Describe_history {

        private TradeHistoryService service() {
            return new TradeHistoryService(tradeHistoryPort, tradeProductPort);
        }

        private TradeHistoryRow row(long tradeId, long buyerId, long sellerId, long productId) {
            return new TradeHistoryRow(tradeId, buyerId, "구매자" + buyerId, sellerId, "판매자" + sellerId,
                    TradeStatus.COMPLETED, LocalDateTime.of(2026, 1, 1, 0, 0), productId, "상품" + productId, 1000);
        }

        @Test
        @DisplayName("내가 구매자면 BUYER 쪽으로 상대를 판매자로, 판매자면 상대를 구매자로 만든다")
        void it_resolves_role_and_counterpart() {
            when(tradeHistoryPort.findByMemberAndStatus(1L, TradeStatus.COMPLETED))
                    .thenReturn(List.of(row(10L, 1L, 2L, 100L), row(11L, 3L, 1L, 101L)));
            when(tradeProductPort.findRepresentativeImages(List.of(100L, 101L)))
                    .thenReturn(Map.of(100L, new TradeImage(7L, "url")));

            List<TradeHistoryItem> items = service().query(new TradeHistoryCriteria(1L, TradeStatus.COMPLETED));

            assertThat(items).hasSize(2);
            assertThat(items.get(0).actorIsBuyer()).isTrue();
            assertThat(items.get(0).otherMemberId()).isEqualTo(2L);
            assertThat(items.get(0).otherNickname()).isEqualTo("판매자2");
            assertThat(items.get(0).image()).isEqualTo(new TradeImage(7L, "url"));
            assertThat(items.get(1).actorIsBuyer()).isFalse();
            assertThat(items.get(1).otherMemberId()).isEqualTo(3L);
            assertThat(items.get(1).image()).isNull();
        }

        @Test
        @DisplayName("거래가 없으면 빈 목록을 반환하고 이미지를 조회하지 않는다")
        void it_skips_images_without_trades() {
            when(tradeHistoryPort.findByMemberAndStatus(1L, TradeStatus.PENDING)).thenReturn(List.of());

            assertThat(service().query(new TradeHistoryCriteria(1L, TradeStatus.PENDING))).isEmpty();
            verifyNoInteractions(tradeProductPort);
        }

        @Test
        @DisplayName("같은 상품의 이미지는 한 번만 조회한다")
        void it_deduplicates_product_ids() {
            when(tradeHistoryPort.findByMemberAndStatus(1L, TradeStatus.COMPLETED))
                    .thenReturn(List.of(row(10L, 1L, 2L, 100L), row(11L, 1L, 3L, 100L)));
            when(tradeProductPort.findRepresentativeImages(List.of(100L))).thenReturn(Map.of());

            service().query(new TradeHistoryCriteria(1L, TradeStatus.COMPLETED));

            verify(tradeProductPort).findRepresentativeImages(List.of(100L));
        }
    }

    @Nested
    @DisplayName("TradeStatisticsService 는")
    class Describe_statistics {

        private TradeStatisticsService service() {
            return new TradeStatisticsService(tradeStatisticsPort, tradePlacePort);
        }

        @Test
        @DisplayName("기간별 본부 통계를 저장소 순서대로 지점 정보와 묶는다(지점이 없으면 null)")
        void it_counts_by_head_with_places() {
            Map<Integer, Long> counts = new LinkedHashMap<>();
            counts.put(2, 5L);
            counts.put(9, 1L);
            when(tradeStatisticsPort.countByHead(eq(7), any(LocalDateTime.class), eq(1))).thenReturn(counts);
            when(tradePlacePort.findPlaces(counts.keySet())).thenReturn(Map.of(2, new TradePlace(2, "첨단")));

            assertThat(service().countByHead(7, 1)).containsExactly(
                    new PlaceTradeCount(new TradePlace(2, "첨단"), 5L),
                    new PlaceTradeCount(null, 1L));
        }

        @Test
        @DisplayName("거래 내역이 없으면 빈 리스트를 반환한다")
        void it_returns_empty() {
            when(tradeStatisticsPort.countByHead(eq(1), any(LocalDateTime.class), eq(1))).thenReturn(Map.of());
            when(tradePlacePort.findPlaces(any())).thenReturn(Map.of());

            assertThat(service().countByHead(1, 1)).isEmpty();
        }

        @Test
        @DisplayName("기간 통계는 시작일 00시부터 종료일 다음날 00시 전까지 센다")
        void it_uses_half_open_range() {
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 1, 31);
            when(tradeStatisticsPort.countByPlace(3, start.atStartOfDay(), LocalDate.of(2026, 2, 1).atStartOfDay())).thenReturn(4L);
            when(tradeStatisticsPort.countByHead(1, start.atStartOfDay(), LocalDate.of(2026, 2, 1).atStartOfDay())).thenReturn(Map.of());
            when(tradePlacePort.findPlaces(any())).thenReturn(Map.of());

            assertThat(service().countByPlace(3, start, end)).isEqualTo(4L);
            assertThat(service().countByHead(1, start, end)).isEmpty();
        }

        @Test
        @DisplayName("최근 기간 지점 통계를 그대로 돌려준다")
        void it_counts_by_place() {
            when(tradeStatisticsPort.countByPlace(eq(30), any(LocalDateTime.class), eq(3))).thenReturn(0L);

            assertThat(service().countByPlace(30, 3)).isZero();
        }

        @Test
        @DisplayName("시작일이 종료일보다 늦거나 null 이면 InvalidTradeStatisticsPeriodException 을 던진다")
        void it_validates_period() {
            LocalDate day = LocalDate.of(2026, 1, 2);

            assertThatThrownBy(() -> service().countByHead(1, day, day.minusDays(1))).isInstanceOf(InvalidTradeStatisticsPeriodException.class);
            assertThatThrownBy(() -> service().countByPlace(1, null, day)).isInstanceOf(InvalidTradeStatisticsPeriodException.class);
            assertThatThrownBy(() -> service().countByPlace(1, day, null)).isInstanceOf(InvalidTradeStatisticsPeriodException.class);
            verifyNoInteractions(tradeStatisticsPort);
        }
    }
}
