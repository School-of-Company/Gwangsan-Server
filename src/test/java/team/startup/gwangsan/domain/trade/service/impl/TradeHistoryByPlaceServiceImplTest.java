package team.startup.gwangsan.domain.trade.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.trade.exception.InvalidTradeStatisticsPeriodException;
import team.startup.gwangsan.domain.trade.presentation.dto.request.constant.Period;
import team.startup.gwangsan.domain.trade.presentation.dto.response.PlaceTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeHistoryByPlaceServiceImpl 단위 테스트")
class TradeHistoryByPlaceServiceImplTest {

    @InjectMocks private TradeHistoryByPlaceServiceImpl service;

    @Mock private TradeCompleteRepository tradeCompleteRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("거래 내역이 있을 때")
        class Context_with_history {

            @Test
            @DisplayName("지점 거래 건수를 반환한다")
            void it_returns_trade_count_by_place() {
                when(tradeCompleteRepository.countByPlaceId(eq(Period.MONTH.getValue()), any(), eq(5))).thenReturn(12L);

                PlaceTradeHistoryResponse result = service.execute(Period.MONTH, 5);

                assertThat(result.count()).isEqualTo(12L);
            }
        }

        @Nested
        @DisplayName("거래 내역이 없을 때")
        class Context_with_no_history {

            @Test
            @DisplayName("count가 0인 응답을 반환한다")
            void it_returns_zero_count() {
                when(tradeCompleteRepository.countByPlaceId(eq(Period.WEEK.getValue()), any(), eq(5))).thenReturn(0L);

                PlaceTradeHistoryResponse result = service.execute(Period.WEEK, 5);

                assertThat(result.count()).isEqualTo(0L);
            }
        }
    }

    @Nested
    @DisplayName("execute(placeId, startDate, endDate) 메서드는")
    class Describe_execute_with_period {

        @Nested
        @DisplayName("정상 기간일 때")
        class Context_with_valid_period {

            @Test
            @DisplayName("기간 내 지점 거래 건수를 반환한다")
            void it_returns_trade_count_by_place_between_dates() {
                LocalDate startDate = LocalDate.of(2026, 6, 1);
                LocalDate endDate = LocalDate.of(2026, 6, 8);

                when(tradeCompleteRepository.countByPlaceIdBetween(
                        eq(5),
                        eq(startDate.atStartOfDay()),
                        eq(endDate.plusDays(1).atStartOfDay())
                )).thenReturn(7L);

                PlaceTradeHistoryResponse result = service.execute(5, startDate, endDate);

                assertThat(result.count()).isEqualTo(7L);
            }
        }

        @Nested
        @DisplayName("시작일이 종료일보다 늦을 때")
        class Context_with_invalid_period {

            @Test
            @DisplayName("InvalidTradeStatisticsPeriodException을 던진다")
            void it_throws_invalid_trade_statistics_period_exception() {
                assertThatThrownBy(() -> service.execute(
                        5,
                        LocalDate.of(2026, 6, 9),
                        LocalDate.of(2026, 6, 8)
                )).isInstanceOf(InvalidTradeStatisticsPeriodException.class);

                verify(tradeCompleteRepository, never()).countByPlaceIdBetween(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("기간이 null일 때")
        class Context_with_null_period {

            @Test
            @DisplayName("InvalidTradeStatisticsPeriodException을 던진다")
            void it_throws_invalid_trade_statistics_period_exception() {
                assertThatThrownBy(() -> service.execute(
                        5,
                        null,
                        LocalDate.of(2026, 6, 8)
                )).isInstanceOf(InvalidTradeStatisticsPeriodException.class);

                assertThatThrownBy(() -> service.execute(
                        5,
                        LocalDate.of(2026, 6, 1),
                        null
                )).isInstanceOf(InvalidTradeStatisticsPeriodException.class);

                verify(tradeCompleteRepository, never()).countByPlaceIdBetween(any(), any(), any());
            }
        }
    }
}
