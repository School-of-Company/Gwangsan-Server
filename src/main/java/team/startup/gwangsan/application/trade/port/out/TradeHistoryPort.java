package team.startup.gwangsan.application.trade.port.out;

import team.startup.gwangsan.application.trade.data.TradeHistoryRow;
import team.startup.gwangsan.domain.trade.model.TradeStatus;

import java.util.List;

public interface TradeHistoryPort {
    /** completedAt 내림차순(null 마지막), id 내림차순. */
    List<TradeHistoryRow> findByMemberAndStatus(long memberId, TradeStatus status);
}
