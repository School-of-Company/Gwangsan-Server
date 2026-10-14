package team.startup.gwangsan.application.trade.port.in;

import team.startup.gwangsan.application.trade.data.TradeStateKey;
import team.startup.gwangsan.application.trade.data.TradeStateSnapshot;

/**
 * 거래 상태를 읽는 단일 공개 지점. 호출자의 트랜잭션 안에서 아직 반영 전인 상품 상태 변경도 본다.
 */
public interface TradeStateQuery {
    TradeStateSnapshot query(TradeStateKey key);
}
