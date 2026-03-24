package team.startup.gwangsan.domain.trade.repository.custom;

import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public interface TradeCompleteCustomRepository {
    Map<Integer, Long> countByHeadId(int period, LocalDateTime now, Integer headId);

    Long countByPlaceId(int period, LocalDateTime now, Integer placeId);

    Optional<TradeComplete> findByProductIdAndStatus(Long productId, TradeStatus status);

    Optional<TradeComplete> findByIdWithProductAndMember(Long id);
}
