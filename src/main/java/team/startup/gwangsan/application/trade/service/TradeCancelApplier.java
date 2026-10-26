package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.trade.data.TradeStateKey;
import team.startup.gwangsan.application.trade.data.TradeStateSnapshot;
import team.startup.gwangsan.application.trade.data.TradeStatusChange;
import team.startup.gwangsan.application.trade.port.in.TradeStateQuery;
import team.startup.gwangsan.application.trade.port.out.*;
import team.startup.gwangsan.domain.trade.model.TradeCancel;
import team.startup.gwangsan.domain.trade.model.TradeComplete;

/**
 * 거래 철회를 실제로 반영하는 단일 지점. 관리자 승인과 양측 동의 자동 철회가 공유한다.
 */
@Service
@RequiredArgsConstructor
public class TradeCancelApplier {

    private final TradeProductPort tradeProductPort;
    private final TradeMemberPort tradeMemberPort;
    private final TradeCancelPort tradeCancelPort;
    private final TradeCompletePort tradeCompletePort;
    private final TradeAlertPort tradeAlertPort;
    private final TradeChatPort tradeChatPort;
    private final TradeStateQuery tradeStateQuery;
    private final TradeStatusPort tradeStatusPort;

    /**
     * 호출 전제: 호출자가 이미 해당 상품 행을 비관적 잠금으로 확보했고, {@code tradeCancel} 을
     * 그 잠금 이후에 처음 읽었다. 잠금 전에 읽은 값을 넘기면 PENDING 검사가 옛 값을 보고
     * 통과해 광산이 두 번 환불된다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void apply(TradeCancel tradeCancel, TradeComplete tradeComplete) {
        tradeCancel.approve();

        long productId = tradeComplete.productId();
        int gwangsan = tradeProductPort.find(productId).gwangsan();
        tradeMemberPort.transferForCancel(tradeComplete.sellerId(), tradeComplete.buyerId(), gwangsan, tradeCancel.id());

        tradeCancelPort.update(tradeCancel);
        tradeComplete.rollBack();
        tradeCompletePort.update(tradeComplete);

        // 상품을 다시 거래 가능 상태로 되돌린다. 빠뜨리면 채팅방의 isCompleted 가
        // 상품 상태에서만 파생되므로 철회 후에도 영구히 "거래 완료"로 남는다.
        tradeProductPort.reopen(productId);

        // 처리된 요청이 관리자 목록에 남으면 승인도 기각도 되지 않는 유령 항목이 된다.
        tradeAlertPort.removeCancelReview(tradeCancel.id());
        // 철회 완료 알림은 buyer 와 seller 양쪽에 receipt 를 만든다.
        tradeAlertPort.recordCancelApproved(tradeCancel.id(), tradeCancel.requesterId(), tradeComplete.buyerId(), tradeComplete.sellerId());

        // 철회가 반영되면 대기 중인 요청도 완료된 요청도 남지 않아 requestedBySeller/requestedAt 이
        // 모두 null 이 된다. 그래야 클라이언트가 거래 카드를 감추고 재요청 버튼을 다시 연다.
        tradeChatPort.findRoomId(productId, tradeComplete.buyerId(), tradeComplete.sellerId())
                .ifPresent(roomId -> {
                    TradeStateSnapshot state = tradeStateQuery.query(
                            new TradeStateKey(productId, tradeComplete.buyerId(), tradeComplete.sellerId()));
                    tradeStatusPort.publish(new TradeStatusChange(roomId, productId, state.completed(),
                            state.reserved(), state.requestedBySeller(), state.requestedAt()));
                });
    }
}
