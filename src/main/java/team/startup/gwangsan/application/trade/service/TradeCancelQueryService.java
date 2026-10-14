package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.trade.port.in.TradeCancelQuery;
import team.startup.gwangsan.application.trade.port.out.TradeCancelPort;

import java.util.Collection;
import java.util.Map;

// ponytail: 조회만 하며 호출자(알림 조회)의 트랜잭션에 참여한다.
@Service
@RequiredArgsConstructor
public class TradeCancelQueryService implements TradeCancelQuery {

    private final TradeCancelPort tradeCancelPort;

    @Override
    public Map<Long, Long> findProductIds(Collection<Long> tradeCancelIds) {
        return tradeCancelIds.isEmpty() ? Map.of() : tradeCancelPort.findProductIds(tradeCancelIds);
    }
}
