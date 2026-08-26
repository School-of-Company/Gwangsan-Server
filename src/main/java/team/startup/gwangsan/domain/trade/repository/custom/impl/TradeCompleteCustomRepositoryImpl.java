package team.startup.gwangsan.domain.trade.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.repository.custom.TradeCompleteCustomRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static team.startup.gwangsan.domain.member.entity.QMember.member;
import static team.startup.gwangsan.domain.member.entity.QMemberDetail.memberDetail;
import static team.startup.gwangsan.domain.place.entity.QHead.head;
import static team.startup.gwangsan.domain.place.entity.QPlace.place;
import static team.startup.gwangsan.domain.trade.entity.QTradeComplete.tradeComplete;

@Repository
@RequiredArgsConstructor
public class TradeCompleteCustomRepositoryImpl implements TradeCompleteCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Integer, Long> countByHeadId(int period, LocalDateTime now, Integer headId) {
        LocalDate today = now.toLocalDate();
        LocalDateTime start = today.minusDays(period - 1L).atStartOfDay();
        LocalDateTime nextDayStart = today.plusDays(1L).atStartOfDay();

        return countByHeadIdBetween(headId, start, nextDayStart);
    }

    @Override
    public Long countByPlaceId(int period, LocalDateTime now, Integer placeId) {
        LocalDate today = now.toLocalDate();
        LocalDateTime start = today.minusDays(period - 1L).atStartOfDay();
        LocalDateTime nextDayStart = today.plusDays(1L).atStartOfDay();

        return countByPlaceIdBetween(placeId, start, nextDayStart);
    }

    @Override
    public Map<Integer, Long> countByHeadIdBetween(Integer headId, LocalDateTime start, LocalDateTime end) {
        var cnt = tradeComplete.id.count();

        return queryFactory
                .select(place.id, cnt)
                .from(place)
                .join(place.head, head)
                .leftJoin(memberDetail).on(memberDetail.place.eq(place))
                .leftJoin(memberDetail.member, member)
                .leftJoin(tradeComplete).on(
                        tradeComplete.seller.eq(member),
                        tradeComplete.completedAt.goe(start),
                        tradeComplete.completedAt.lt(end),
                        tradeComplete.status.eq(TradeStatus.COMPLETED))
                .where(head.id.eq(headId))
                .groupBy(place.id)
                .orderBy(place.id.asc())
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> t.get(place.id),
                        t -> t.get(cnt),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Long countByPlaceIdBetween(Integer placeId, LocalDateTime start, LocalDateTime end) {
        Long count = queryFactory
                .select(tradeComplete.id.count())
                .from(place)
                .leftJoin(memberDetail).on(memberDetail.place.eq(place))
                .leftJoin(memberDetail.member, member)
                .leftJoin(tradeComplete).on(
                        tradeComplete.seller.eq(member),
                        tradeComplete.completedAt.goe(start),
                        tradeComplete.completedAt.lt(end),
                        tradeComplete.status.eq(TradeStatus.COMPLETED))
                .where(place.id.eq(placeId))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public Optional<TradeComplete> findByProductIdAndStatus(Long productId, TradeStatus status) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(tradeComplete)
                        .join(tradeComplete.product).fetchJoin()
                        .where(
                                tradeComplete.product.id.eq(productId),
                                tradeComplete.status.eq(status)
                        )
                        .limit(1)
                        .fetchOne()
        );
    }

    @Override
    public Optional<TradeComplete> findByIdWithProductAndMember(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(tradeComplete)
                        .join(tradeComplete.product).fetchJoin()
                        .join(tradeComplete.buyer).fetchJoin()
                        .join(tradeComplete.seller).fetchJoin()
                        .where(tradeComplete.id.eq(id))
                        .fetchOne()
        );
    }

    @Override
    public List<TradeComplete> findAllByMemberAndStatus(Member member, TradeStatus status) {
        return queryFactory
                .selectFrom(tradeComplete)
                .join(tradeComplete.product).fetchJoin()
                .where(
                        tradeComplete.buyer.eq(member).or(tradeComplete.seller.eq(member)),
                        tradeComplete.status.eq(status)
                )
                .orderBy(tradeComplete.completedAt.desc().nullsLast(), tradeComplete.id.desc())
                .fetch();
    }
}
