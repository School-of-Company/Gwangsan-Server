package team.startup.gwangsan.domain.trade.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.repository.custom.TradeCancelCustomRepository;

import java.util.List;
import java.util.Optional;

import static team.startup.gwangsan.domain.trade.entity.QTradeCancel.tradeCancel;
import static team.startup.gwangsan.domain.trade.entity.QTradeComplete.tradeComplete;

@Repository
@RequiredArgsConstructor
public class TradeCancelCustomRepositoryImpl implements TradeCancelCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<TradeCancel> findAllByIdIn(List<Long> ids) {
        return queryFactory
                .selectDistinct(tradeCancel)
                .from(tradeCancel)
                .join(tradeCancel.tradeComplete).fetchJoin()
                .join(tradeCancel.tradeComplete.product).fetchJoin()
                .where(tradeCancel.id.in(ids))
                .fetch();
    }

    @Override
    public Optional<TradeCancel> findByIdWithTradeCompleteAndMember(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(tradeCancel)
                        .join(tradeCancel.tradeComplete, tradeComplete).fetchJoin()
                        .join(tradeComplete.buyer).fetchJoin()
                        .join(tradeComplete.seller).fetchJoin()
                        .where(tradeCancel.id.eq(id))
                        .fetchOne()
        );
    }

    @Override
    public Optional<TradeCancel> findByIdWithMember(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(tradeCancel)
                        .join(tradeCancel.member).fetchJoin()
                        .where(tradeCancel.id.eq(id))
                        .fetchOne()
        );
    }

    @Override
    public Optional<TradeCancel> findByIdWithTradeCompleteAndBuyerAndSellerAndProduct(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(tradeCancel).distinct()
                        .join(tradeCancel.tradeComplete, tradeComplete).fetchJoin()
                        .join(tradeComplete.seller).fetchJoin()
                        .join(tradeComplete.buyer).fetchJoin()
                        .join(tradeComplete.product).fetchJoin()
                        .where(tradeCancel.id.eq(id))
                        .fetchOne()
        );
    }
}
