package team.startup.gwangsan.domain.post.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.post.repository.custom.TradeCompleteCustomRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import static team.startup.gwangsan.domain.member.entity.QMember.member;
import static team.startup.gwangsan.domain.member.entity.QMemberDetail.memberDetail;
import static team.startup.gwangsan.domain.place.entity.QHead.head;
import static team.startup.gwangsan.domain.place.entity.QPlace.place;
import static team.startup.gwangsan.domain.post.entity.QTradeComplete.tradeComplete;

@Repository
@RequiredArgsConstructor
public class TradeCompleteCustomRepositoryImpl implements TradeCompleteCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Integer, Long> countByHeadId(int period, LocalDateTime now, Integer headId) {
        var cnt = tradeComplete.id.count();

        LocalDate today = now.toLocalDate();
        LocalDateTime start = today.minusDays(period - 1L).atStartOfDay();
        LocalDateTime nextDayStart = today.plusDays(1L).atStartOfDay();

        return queryFactory
                .select(place.id, cnt)
                .from(place)
                .join(place.head, head)
                .leftJoin(memberDetail).on(memberDetail.place.eq(place))
                .leftJoin(memberDetail.member, member)
                .leftJoin(tradeComplete).on(
                        tradeComplete.product.member.eq(member),
                        tradeComplete.completedAt.goe(start),
                        tradeComplete.completedAt.lt(nextDayStart))
                .where(head.id.eq(headId))
                .groupBy(place.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> t.get(place.id),
                        t -> t.get(cnt)
                ));
    }

    @Override
    public Long countByPlaceId(int period, LocalDateTime now, Integer placeId) {
        LocalDate today = now.toLocalDate();
        LocalDateTime start = today.minusDays(period - 1L).atStartOfDay();
        LocalDateTime nextDayStart = today.plusDays(1L).atStartOfDay();

        Long count = queryFactory
                .select(tradeComplete.id.count())
                .from(place)
                .leftJoin(memberDetail).on(memberDetail.place.eq(place))
                .leftJoin(memberDetail.member, member)
                .leftJoin(tradeComplete).on(
                        tradeComplete.product.member.eq(member),
                        tradeComplete.completedAt.goe(start),
                        tradeComplete.completedAt.lt(nextDayStart))
                .where(place.id.eq(placeId))
                .fetchOne();

        return count != null ? count : 0L;
    }
}
