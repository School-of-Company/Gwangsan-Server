package team.startup.gwangsan.application.trade.port.out;

import java.util.List;

/** 커밋 후 푸시. 실패해도 거래는 유지된다. */
public interface TradePushPort {
    void notifyCompleted(List<Long> memberIds, long productId);
}
