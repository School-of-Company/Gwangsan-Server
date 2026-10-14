package team.startup.gwangsan.adapter.persistence.trade;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.trade.data.TradeHistoryRow;
import team.startup.gwangsan.application.trade.port.out.TradeHistoryPort;
import team.startup.gwangsan.application.trade.port.out.TradeStatisticsPort;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.trade.model.TradeStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 이력/통계 조회. 회원·상품·지점 join 은 기존 QueryDSL 쿼리를 그대로 쓴다. */
@Component
@RequiredArgsConstructor
public class TradeQueryPersistenceAdapter implements TradeHistoryPort, TradeStatisticsPort {

    private final TradeCompleteJpaRepository tradeCompleteJpaRepository;
    private final EntityManager entityManager;

    @Override
    public List<TradeHistoryRow> findByMemberAndStatus(long memberId, TradeStatus status) {
        return tradeCompleteJpaRepository.findAllByMemberAndStatus(entityManager.getReference(MemberJpaEntity.class, memberId), status)
                .stream()
                .map(t -> new TradeHistoryRow(
                        t.getId(),
                        t.getBuyer().getId(), t.getBuyer().getNickname(),
                        t.getSeller().getId(), t.getSeller().getNickname(),
                        t.getStatus(), t.getCompletedAt(),
                        t.getProduct().getId(), t.getProduct().getTitle(), t.getProduct().getGwangsan()))
                .toList();
    }

    @Override
    public Map<Integer, Long> countByHead(int periodDays, LocalDateTime now, Integer headId) {
        return tradeCompleteJpaRepository.countByHeadId(periodDays, now, headId);
    }

    @Override
    public Map<Integer, Long> countByHead(Integer headId, LocalDateTime start, LocalDateTime end) {
        return tradeCompleteJpaRepository.countByHeadIdBetween(headId, start, end);
    }

    @Override
    public Long countByPlace(int periodDays, LocalDateTime now, Integer placeId) {
        return tradeCompleteJpaRepository.countByPlaceId(periodDays, now, placeId);
    }

    @Override
    public Long countByPlace(Integer placeId, LocalDateTime start, LocalDateTime end) {
        return tradeCompleteJpaRepository.countByPlaceIdBetween(placeId, start, end);
    }
}
