package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.trade.data.CompletedTrade;
import team.startup.gwangsan.application.trade.data.TradeStateKey;
import team.startup.gwangsan.application.trade.data.TradeStateSnapshot;
import team.startup.gwangsan.application.trade.port.in.CompletedTradeQuery;
import team.startup.gwangsan.application.trade.port.in.TradeStateQuery;
import team.startup.gwangsan.application.trade.port.out.TradeCompletePort;
import team.startup.gwangsan.application.trade.port.out.TradeProductPort;
import team.startup.gwangsan.domain.trade.model.TradeState;

import java.util.Optional;

/**
 * 거래 상태 공개 조회. 트랜잭션을 열지 않고 호출자의 트랜잭션에 그대로 참여한다(기존 TradeStateReader 와 같음).
 */
@Service
@RequiredArgsConstructor
public class TradeStateService implements TradeStateQuery, CompletedTradeQuery {

    private final TradeProductPort tradeProductPort;
    private final TradeCompletePort tradeCompletePort;

    @Override
    public TradeStateSnapshot query(TradeStateKey key) {
        TradeState state = TradeState.of(
                tradeProductPort.find(key.productId()).phase(),
                tradeCompletePort.findRequestStates(key.productId(), key.buyerId(), key.sellerId()));
        return new TradeStateSnapshot(state.completed(), state.reserved(), state.requestedBySeller(), state.requestedAt());
    }

    @Override
    public Optional<CompletedTrade> query(long productId) {
        return tradeCompletePort.findCompleted(productId)
                .map(t -> new CompletedTrade(t.id(), t.buyerId(), t.sellerId(), t.completedAt()));
    }
}
