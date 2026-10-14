package team.startup.gwangsan.application.trade.port.out;

import team.startup.gwangsan.application.trade.data.TradeStatusChange;

/** 커밋 후 비동기 전달. 서버 재시도 없음. */
public interface TradeStatusPort {
    void publish(TradeStatusChange change);
}
