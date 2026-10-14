package team.startup.gwangsan.application.trade.port.in;

import team.startup.gwangsan.application.trade.data.CompletedTrade;

import java.util.Optional;

public interface CompletedTradeQuery {
    Optional<CompletedTrade> query(long productId);
}
