package team.startup.gwangsan.domain.trade.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.domain.trade.presentation.dto.request.constant.Period;
import team.startup.gwangsan.domain.trade.presentation.dto.response.HeadTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeHistoryByHeadServiceImpl 단위 테스트")
class TradeHistoryByHeadServiceImplTest {

    @InjectMocks private TradeHistoryByHeadServiceImpl service;

    @Mock private TradeCompleteRepository tradeCompleteRepository;
    @Mock private PlaceRepository placeRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("거래 내역이 있을 때")
        class Context_with_history {

            @Test
            @DisplayName("지점별 거래 건수를 반환한다")
            void it_returns_trade_history_by_head() {
                when(tradeCompleteRepository.countByHeadId(eq(7), any(), eq(1)))
                        .thenReturn(Map.of(10, 5L, 11, 3L));

                Place place1 = mock(Place.class);
                when(place1.getId()).thenReturn(10);
                when(place1.getName()).thenReturn("수완지점");

                Place place2 = mock(Place.class);
                when(place2.getId()).thenReturn(11);
                when(place2.getName()).thenReturn("하남지점");

                when(placeRepository.findAllById(any())).thenReturn(List.of(place1, place2));

                List<HeadTradeHistoryResponse> result = service.execute(Period.WEEK, 1);

                assertThat(result).hasSize(2);
            }
        }

        @Nested
        @DisplayName("거래 내역이 없을 때")
        class Context_with_no_history {

            @Test
            @DisplayName("빈 리스트를 반환한다")
            void it_returns_empty_list() {
                when(tradeCompleteRepository.countByHeadId(eq(1), any(), eq(1)))
                        .thenReturn(Map.of());
                when(placeRepository.findAllById(any())).thenReturn(List.of());

                List<HeadTradeHistoryResponse> result = service.execute(Period.DAY, 1);

                assertThat(result).isEmpty();
            }
        }
    }
}
