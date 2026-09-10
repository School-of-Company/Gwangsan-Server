package team.startup.gwangsan.domain.trade.service;

import team.startup.gwangsan.domain.trade.presentation.dto.response.TradeCancelResponse;

import java.util.List;

public interface TradeCancelService {
    TradeCancelResponse execute(Long productId, String reason, List<Long> imageIds);
}
