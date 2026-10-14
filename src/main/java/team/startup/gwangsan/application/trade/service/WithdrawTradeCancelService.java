package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.trade.data.WithdrawTradeCancelCommand;
import team.startup.gwangsan.application.trade.port.in.WithdrawTradeCancelUseCase;
import team.startup.gwangsan.application.trade.port.out.TradeCancelPort;
import team.startup.gwangsan.application.trade.port.out.TradeCompletePort;
import team.startup.gwangsan.application.trade.port.out.TradeProductPort;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.model.TradeCancel;
import team.startup.gwangsan.domain.trade.model.TradeComplete;

@Service
@RequiredArgsConstructor
public class WithdrawTradeCancelService implements WithdrawTradeCancelUseCase {

    private final TradeProductPort tradeProductPort;
    private final TradeCompletePort tradeCompletePort;
    private final TradeCancelPort tradeCancelPort;

    /** 잠금과 격리 수준의 이유는 {@link RequestTradeCancelService} 주석 참고. */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void execute(WithdrawTradeCancelCommand command) {
        tradeProductPort.lockIncludingDeleted(command.productId());

        TradeComplete tradeComplete = tradeCompletePort.findCompleted(command.productId())
                .orElseThrow(NotFoundTradeCompleteException::new);
        TradeCancel tradeCancel = tradeCancelPort.findPendingByTrade(tradeComplete.id())
                .orElseThrow(NotFoundTradeCancelException::new);

        tradeCancel.withdraw(command.actorId());
        tradeCancelPort.update(tradeCancel);
        tradeCancelPort.deleteImages(tradeCancel.id());
    }
}
