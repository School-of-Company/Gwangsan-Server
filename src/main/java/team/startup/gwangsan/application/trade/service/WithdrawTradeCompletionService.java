package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.trade.data.TradeCompletionCommand;
import team.startup.gwangsan.application.trade.data.TradeStateKey;
import team.startup.gwangsan.application.trade.data.TradeStateSnapshot;
import team.startup.gwangsan.application.trade.data.TradeStatusChange;
import team.startup.gwangsan.application.trade.port.in.TradeStateQuery;
import team.startup.gwangsan.application.trade.port.in.WithdrawTradeCompletionUseCase;
import team.startup.gwangsan.application.trade.port.out.TradeChatPort;
import team.startup.gwangsan.application.trade.port.out.TradeCompletePort;
import team.startup.gwangsan.application.trade.port.out.TradeMemberPort;
import team.startup.gwangsan.application.trade.port.out.TradeProductPort;
import team.startup.gwangsan.application.trade.port.out.TradeStatusPort;
import team.startup.gwangsan.domain.trade.exception.CannotSelectSelfException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.model.TradeComplete;
import team.startup.gwangsan.domain.trade.model.TradeProduct;
import team.startup.gwangsan.domain.trade.model.TradeSides;

@Service
@RequiredArgsConstructor
public class WithdrawTradeCompletionService implements WithdrawTradeCompletionUseCase {

    private final TradeProductPort tradeProductPort;
    private final TradeMemberPort tradeMemberPort;
    private final TradeCompletePort tradeCompletePort;
    private final TradeChatPort tradeChatPort;
    private final TradeStateQuery tradeStateQuery;
    private final TradeStatusPort tradeStatusPort;

    // ponytail: 기존과 같이 기본 격리 수준이다. 요청/확정 경로와 같은 상품 잠금을 먼저 잡는다.
    @Override
    @Transactional
    public void execute(TradeCompletionCommand command) {
        long actorId = command.actorId();
        long otherMemberId = command.otherMemberId();
        if (actorId == otherMemberId) {
            throw new CannotSelectSelfException();
        }

        TradeProduct product = tradeProductPort.lockActive(command.productId());
        product.validateNotCompleted();
        tradeMemberPort.requireMember(otherMemberId);

        TradeSides sides = product.sides(actorId, otherMemberId);
        TradeComplete pending = tradeCompletePort.findPending(product.productId(), sides.buyerId(), sides.sellerId())
                .orElseThrow(NotFoundTradeCompleteException::new);
        pending.validateWithdrawableBy(sides.actorIsBuyer());

        long roomId = tradeChatPort.requireRoomId(product.productId(), sides.buyerId(), sides.sellerId());

        tradeCompletePort.delete(pending.id());

        // 삭제 이후 상태를 단일 지점으로 다시 읽어, 조회 응답과 같은 값을 전달한다.
        TradeStateSnapshot state = tradeStateQuery.query(new TradeStateKey(product.productId(), sides.buyerId(), sides.sellerId()));
        tradeStatusPort.publish(new TradeStatusChange(roomId, product.productId(), state.completed(), state.reserved(),
                state.requestedBySeller(), state.requestedAt()));
    }
}
