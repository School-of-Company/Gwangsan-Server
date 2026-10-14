package team.startup.gwangsan.adapter.persistence.trade;

import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.trade.model.TradeStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TradeCompleteCustomRepository {
    Map<Integer, Long> countByHeadId(int period, LocalDateTime now, Integer headId);

    Long countByPlaceId(int period, LocalDateTime now, Integer placeId);

    Map<Integer, Long> countByHeadIdBetween(Integer headId, LocalDateTime start, LocalDateTime end);

    Long countByPlaceIdBetween(Integer placeId, LocalDateTime start, LocalDateTime end);

    Optional<TradeCompleteJpaEntity> findByProductIdAndStatus(Long productId, TradeStatus status);

    Optional<TradeCompleteJpaEntity> findByIdWithProductAndMember(Long id);

    List<TradeCompleteJpaEntity> findAllByMemberAndStatus(MemberJpaEntity member, TradeStatus status);
}
