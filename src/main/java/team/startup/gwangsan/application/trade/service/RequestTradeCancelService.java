package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.trade.data.RequestTradeCancelCommand;
import team.startup.gwangsan.application.trade.data.TradeCancelResult;
import team.startup.gwangsan.application.trade.port.in.RequestTradeCancelUseCase;
import team.startup.gwangsan.application.trade.port.out.TradeAlertPort;
import team.startup.gwangsan.application.trade.port.out.TradeCancelPort;
import team.startup.gwangsan.application.trade.port.out.TradeCompletePort;
import team.startup.gwangsan.application.trade.port.out.TradeProductPort;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.exception.TradeParticipantOnlyException;
import team.startup.gwangsan.domain.trade.model.TradeCancel;
import team.startup.gwangsan.domain.trade.model.TradeComplete;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RequestTradeCancelService implements RequestTradeCancelUseCase {

    private final TradeProductPort tradeProductPort;
    private final TradeCompletePort tradeCompletePort;
    private final TradeCancelPort tradeCancelPort;
    private final TradeCancelApplier tradeCancelApplier;
    private final TradeAlertPort tradeAlertPort;

    /**
     * READ_COMMITTED 를 쓰는 이유: 기본값인 REPEATABLE READ 에서는 첫 SELECT 시점에 read view 가
     * 고정된다. 그러면 상품 잠금을 기다리는 동안 상대방 트랜잭션이 철회를 커밋해도 이 트랜잭션은
     * 계속 옛 값을 보고 광산이 두 번 환불된다. 잠금을 잡은 뒤의 조회가 최신 커밋본을 보게 해야 한다.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TradeCancelResult execute(RequestTradeCancelCommand command) {
        long actorId = command.actorId();

        // 철회 요청·승인·기각·요청 취소가 모두 이 잠금을 거쳐 직렬화된다.
        // 첫 요청 시점에는 철회 행이 아직 없으므로 상시 존재하는 상품 행을 잠근다.
        tradeProductPort.lockIncludingDeleted(command.productId());

        TradeComplete tradeComplete = tradeCompletePort.findCompleted(command.productId())
                .orElseThrow(NotFoundTradeCompleteException::new);
        if (!tradeComplete.isParticipant(actorId)) {
            throw new TradeParticipantOnlyException();
        }

        Optional<TradeCancel> pending = tradeCancelPort.findPendingByTrade(tradeComplete.id());
        if (pending.isPresent()) {
            TradeCancel requested = pending.get();
            requested.validateAgreementBy(actorId);
            // 상대방이 같은 거래의 철회를 요청했다는 것은 양측이 동의했다는 뜻이다.
            // 두 번째 요청은 동의 신호로만 쓰고 별도 철회 요청으로 남기지 않는다.
            tradeCancelApplier.apply(requested, tradeComplete);
            return new TradeCancelResult(true);
        }

        TradeCancel created = tradeCancelPort.create(
                TradeCancel.request(tradeComplete.id(), actorId, command.reason()), command.imageIds());
        tradeAlertPort.requestCancelReview(created.id(), actorId);
        return new TradeCancelResult(false);
    }
}
