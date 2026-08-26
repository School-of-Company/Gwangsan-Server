package team.startup.gwangsan.domain.trade.repository.custom;

import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TradeCompleteCustomRepository {
    Map<Integer, Long> countByHeadId(int period, LocalDateTime now, Integer headId);

    Long countByPlaceId(int period, LocalDateTime now, Integer placeId);

    Map<Integer, Long> countByHeadIdBetween(Integer headId, LocalDateTime start, LocalDateTime end);

    Long countByPlaceIdBetween(Integer placeId, LocalDateTime start, LocalDateTime end);

    Optional<TradeComplete> findByProductIdAndStatus(Long productId, TradeStatus status);

    Optional<TradeComplete> findByIdWithProductAndMember(Long id);

    List<TradeComplete> findAllByMemberAndStatus(Member member, TradeStatus status);
}
