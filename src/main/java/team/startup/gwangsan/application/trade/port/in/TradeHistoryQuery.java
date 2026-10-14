package team.startup.gwangsan.application.trade.port.in;

import team.startup.gwangsan.application.trade.data.TradeHistoryCriteria;
import team.startup.gwangsan.application.trade.data.TradeHistoryItem;

import java.util.List;

public interface TradeHistoryQuery {
    List<TradeHistoryItem> query(TradeHistoryCriteria criteria);
}
