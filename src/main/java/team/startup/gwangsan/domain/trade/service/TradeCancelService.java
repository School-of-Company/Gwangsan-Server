package team.startup.gwangsan.domain.trade.service;

import java.util.List;

public interface TradeCancelService {
    void execute(Long productId, String reason, List<Long> imageIds);
}
