package team.startup.gwangsan.domain.trade.service;

import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.presentation.dto.response.GetTradeHistoryResponse;

import java.util.List;

public interface FindMyTradeHistoryService {
    List<GetTradeHistoryResponse> execute(TradeStatus status);
}
