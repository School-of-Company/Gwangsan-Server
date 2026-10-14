package team.startup.gwangsan.adapter.persistence.trade;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class TradeCancelCustomRepositoryImpl implements TradeCancelCustomRepository {

    private static final QTradeCompleteJpaEntity tradeComplete = QTradeCompleteJpaEntity.tradeCompleteJpaEntity;
    private static final QTradeCancelJpaEntity tradeCancel = QTradeCancelJpaEntity.tradeCancelJpaEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<TradeCancelJpaEntity> findAllByIdIn(List<Long> ids) {
        return queryFactory
                .selectDistinct(tradeCancel)
                .from(tradeCancel)
                .join(tradeCancel.tradeComplete).fetchJoin()
                .join(tradeCancel.tradeComplete.product).fetchJoin()
                .where(tradeCancel.id.in(ids))
                .fetch();
    }

    @Override
    public Optional<TradeCancelJpaEntity> findByIdWithTradeCompleteAndMember(Long id) {
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
    public Optional<TradeCancelJpaEntity> findByIdWithMember(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(tradeCancel)
                        .join(tradeCancel.member).fetchJoin()
                        .where(tradeCancel.id.eq(id))
                        .fetchOne()
        );
    }

    @Override
    public Optional<TradeCancelJpaEntity> findByIdWithTradeCompleteAndBuyerAndSellerAndProduct(Long id) {
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
