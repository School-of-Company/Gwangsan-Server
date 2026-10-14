package team.startup.gwangsan.application.trade.data;

import team.startup.gwangsan.domain.trade.model.TradeStatus;

public record TradeHistoryCriteria(long actorId, TradeStatus status) {
}
