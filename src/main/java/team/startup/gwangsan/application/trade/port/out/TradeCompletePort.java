package team.startup.gwangsan.application.trade.port.out;

import team.startup.gwangsan.domain.trade.model.TradeComplete;
import team.startup.gwangsan.domain.trade.model.TradeRequestState;

import java.util.List;
import java.util.Optional;

/** 거래 완료 요청 저장. 변경은 update/delete 로 명시해 같은 트랜잭션의 관리 엔티티에 반영한다. */
public interface TradeCompletePort {
    Optional<TradeComplete> findById(long tradeId);

    Optional<TradeComplete> findPending(long productId, long buyerId, long sellerId);

    Optional<TradeComplete> findCompleted(long productId);

    List<TradeRequestState> findRequestStates(long productId, long buyerId, long sellerId);

    TradeComplete create(TradeComplete tradeComplete);

    void update(TradeComplete tradeComplete);

    void delete(long tradeId);

    /** 확정 후 같은 상품에 남은 다른 대기 요청을 지운다. 확정한 요청은 update 로 이미 COMPLETED 여야 한다. */
    void deleteOtherPending(long productId);
}
