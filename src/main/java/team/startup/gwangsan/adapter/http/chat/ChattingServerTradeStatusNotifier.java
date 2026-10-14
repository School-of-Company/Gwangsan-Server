package team.startup.gwangsan.adapter.http.chat;

import team.startup.gwangsan.adapter.event.trade.TradeStatusChangedEvent;

public interface ChattingServerTradeStatusNotifier {
    void notifyTradeStatusChanged(TradeStatusChangedEvent event);
}
