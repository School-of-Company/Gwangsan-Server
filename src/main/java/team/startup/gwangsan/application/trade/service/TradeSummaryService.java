package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.trade.data.TradeCancelParties;
import team.startup.gwangsan.application.trade.data.TradeCompleteSummary;
import team.startup.gwangsan.application.trade.port.in.TradeSummaryQuery;
import team.startup.gwangsan.application.trade.port.out.TradeCancelPort;
import team.startup.gwangsan.application.trade.port.out.TradeCompletePort;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;

/** 조회 전용. 호출자(알림 저장) 트랜잭션이 있으면 참여하고, 없으면 지연 로딩을 위해 읽기 트랜잭션을 연다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeSummaryService implements TradeSummaryQuery {

    private final TradeCompletePort tradeCompletePort;
    private final TradeCancelPort tradeCancelPort;

    @Override
    public TradeCompleteSummary getTradeComplete(long tradeCompleteId) {
        return tradeCompletePort.findSummary(tradeCompleteId).orElseThrow(NotFoundTradeCompleteException::new);
    }

    @Override
    public TradeCancelParties getCancelParties(long tradeCancelId) {
        return tradeCancelPort.findParties(tradeCancelId).orElseThrow(NotFoundTradeCancelException::new);
    }
}
