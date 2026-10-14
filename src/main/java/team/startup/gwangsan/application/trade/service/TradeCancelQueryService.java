package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.trade.data.TradeCancelImageView;
import team.startup.gwangsan.application.trade.data.TradeCancelView;
import team.startup.gwangsan.application.trade.port.in.TradeCancelQuery;
import team.startup.gwangsan.application.trade.port.out.TradeCancelPort;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// ponytail: 조회만 하며 호출자(알림·관리자 조회)의 트랜잭션에 참여한다.
@Service
@RequiredArgsConstructor
public class TradeCancelQueryService implements TradeCancelQuery {

    private final TradeCancelPort tradeCancelPort;

    @Override
    public Map<Long, Long> findProductIds(Collection<Long> tradeCancelIds) {
        return tradeCancelIds.isEmpty() ? Map.of() : tradeCancelPort.findProductIds(tradeCancelIds);
    }

    @Override
    public List<TradeCancelView> findAll(Collection<Long> tradeCancelIds) {
        return tradeCancelIds.isEmpty() ? List.of() : tradeCancelPort.findViews(tradeCancelIds);
    }

    @Override
    public Map<Long, List<TradeCancelImageView>> findImages(Collection<Long> tradeCancelIds) {
        return tradeCancelIds.isEmpty() ? Map.of() : tradeCancelPort.findImageViews(tradeCancelIds);
    }

    @Override
    public Optional<Long> findRequesterId(long tradeCancelId) {
        return tradeCancelPort.findRequesterId(tradeCancelId);
    }
}
