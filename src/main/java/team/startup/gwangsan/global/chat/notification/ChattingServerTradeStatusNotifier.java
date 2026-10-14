package team.startup.gwangsan.global.chat.notification;

import team.startup.gwangsan.adapter.event.trade.TradeStatusChangedEvent;

public interface ChattingServerTradeStatusNotifier {
    void notifyTradeStatusChanged(TradeStatusChangedEvent event);
}
