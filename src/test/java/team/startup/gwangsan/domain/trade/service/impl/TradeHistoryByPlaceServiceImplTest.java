package team.startup.gwangsan.domain.trade.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.trade.presentation.dto.request.constant.Period;
import team.startup.gwangsan.domain.trade.presentation.dto.response.PlaceTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;

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
                when(tradeCompleteRepository.countByPlaceId(eq(30), any(), eq(5))).thenReturn(12L);

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
                when(tradeCompleteRepository.countByPlaceId(anyInt(), any(), anyInt())).thenReturn(0L);

                PlaceTradeHistoryResponse result = service.execute(Period.WEEK, 5);

                assertThat(result.count()).isEqualTo(0L);
            }
        }
    }
}
