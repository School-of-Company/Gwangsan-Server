package team.startup.gwangsan.application.trade.port.in;

import java.util.Collection;
import java.util.Map;

/** 철회 요청 공개 조회. */
public interface TradeCancelQuery {
    /** 철회 요청 ID → 대상 상품 ID. 없는 ID 는 결과에서 빠진다. 빈 입력이면 조회하지 않는다. */
    Map<Long, Long> findProductIds(Collection<Long> tradeCancelIds);
}
