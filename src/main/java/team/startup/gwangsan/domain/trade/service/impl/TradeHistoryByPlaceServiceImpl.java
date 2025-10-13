package team.startup.gwangsan.domain.trade.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.trade.presentation.dto.request.constant.Period;
import team.startup.gwangsan.domain.trade.presentation.dto.response.PlaceTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.service.TradeHistoryByPlaceService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TradeHistoryByPlaceServiceImpl implements TradeHistoryByPlaceService {

    private final TradeCompleteRepository tradeCompleteRepository;

    @Override
    public PlaceTradeHistoryResponse execute(Period period, Integer placeId) {
        return new PlaceTradeHistoryResponse(
                tradeCompleteRepository.countByPlaceId(
                        period.getValue(), LocalDateTime.now(), placeId)
        );
    }
}
