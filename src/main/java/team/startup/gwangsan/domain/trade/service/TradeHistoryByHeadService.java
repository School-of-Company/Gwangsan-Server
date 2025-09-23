package team.startup.gwangsan.domain.trade.service;

import team.startup.gwangsan.domain.trade.presentation.dto.request.constant.Period;
import team.startup.gwangsan.domain.trade.presentation.dto.response.HeadTradeHistoryResponse;

import java.util.List;

public interface TradeHistoryByHeadService {
    List<HeadTradeHistoryResponse> execute(Period period, Integer headId);
}
