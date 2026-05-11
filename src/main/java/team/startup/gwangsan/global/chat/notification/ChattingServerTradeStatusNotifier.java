package team.startup.gwangsan.global.chat.notification;

import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

public interface ChattingServerTradeStatusNotifier {
    void notifyTradeStatusChanged(TradeStatusChangedEvent event);
}
