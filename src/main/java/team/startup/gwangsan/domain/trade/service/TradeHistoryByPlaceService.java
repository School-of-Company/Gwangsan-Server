package team.startup.gwangsan.domain.trade.service;

import team.startup.gwangsan.domain.trade.presentation.dto.request.constant.Period;
import team.startup.gwangsan.domain.trade.presentation.dto.response.PlaceTradeHistoryResponse;

public interface TradeHistoryByPlaceService {
    PlaceTradeHistoryResponse execute(Period period, Integer placeId);
}
