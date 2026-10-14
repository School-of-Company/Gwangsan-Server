package team.startup.gwangsan.application.trade.port.out;

import team.startup.gwangsan.application.trade.data.TradeCancelImageView;
import team.startup.gwangsan.application.trade.data.TradeCancelView;
import team.startup.gwangsan.domain.trade.model.TradeCancel;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TradeCancelPort {
    Optional<TradeCancel> findPendingByTrade(long tradeCompleteId);

    /** 잠금 전에 잠글 대상을 알아내는 용도. 철회 요청을 영속 상태로 올리지 않는다. */
    Optional<Long> findProductId(long tradeCancelId);

    Optional<TradeCancel> findById(long tradeCancelId);

    /** 연관 fetch join 없이 철회 요청 하나를 읽는다(기각 경로, 호출자가 잠금 후 호출). */
    Optional<TradeCancel> findBasicById(long tradeCancelId);

    List<TradeCancelView> findViews(Collection<Long> tradeCancelIds);

    Map<Long, List<TradeCancelImageView>> findImageViews(Collection<Long> tradeCancelIds);

    Optional<Long> findRequesterId(long tradeCancelId);

    /** 철회 요청 ID → 상품 ID (1 쿼리). */
    Map<Long, Long> findProductIds(Collection<Long> tradeCancelIds);

    TradeCancel create(TradeCancel tradeCancel, List<Long> imageIds);

    void update(TradeCancel tradeCancel);

    void deleteImages(long tradeCancelId);
}
