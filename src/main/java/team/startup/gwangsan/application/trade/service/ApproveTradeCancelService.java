package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.trade.data.ApproveTradeCancelCommand;
import team.startup.gwangsan.application.trade.port.in.ApproveTradeCancelUseCase;
import team.startup.gwangsan.application.trade.port.out.TradeCancelPort;
import team.startup.gwangsan.application.trade.port.out.TradeCompletePort;
import team.startup.gwangsan.application.trade.port.out.TradeProductPort;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.model.TradeCancel;
import team.startup.gwangsan.domain.trade.model.TradeComplete;

@Service
@RequiredArgsConstructor
public class ApproveTradeCancelService implements ApproveTradeCancelUseCase {

    private final TradeProductPort tradeProductPort;
    private final TradeCancelPort tradeCancelPort;
    private final TradeCompletePort tradeCompletePort;
    private final TradeCancelApplier tradeCancelApplier;

    /**
     * 잠금 순서가 핵심이다. 철회 요청을 잠금 전에 엔티티로 읽으면 그 값이 1차 캐시에 남아,
     * 양측 동의로 이미 철회된 뒤에도 PENDING 으로 보여 광산이 두 번 환불된다. 그래서 상품 ID 만
     * 스칼라로 먼저 읽어 잠금을 잡고, 철회 요청은 그 뒤에 처음 로드한다.
     * READ_COMMITTED 이유는 {@link RequestTradeCancelService} 주석 참고.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void execute(ApproveTradeCancelCommand command) {
        long tradeCancelId = command.tradeCancelId();
        long productId = tradeCancelPort.findProductId(tradeCancelId)
                .orElseThrow(NotFoundTradeCancelException::new);

        tradeProductPort.lockIncludingDeleted(productId);

        TradeCancel tradeCancel = tradeCancelPort.findById(tradeCancelId)
                .orElseThrow(NotFoundTradeCancelException::new);
        TradeComplete tradeComplete = tradeCompletePort.findById(tradeCancel.tradeCompleteId())
                .orElseThrow(NotFoundTradeCompleteException::new);

        command.authorizer().authorize(tradeComplete.buyerId(), tradeComplete.sellerId());

        tradeCancelApplier.apply(tradeCancel, tradeComplete);
    }
}
