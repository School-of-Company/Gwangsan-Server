package team.startup.gwangsan.adapter.persistence.trade;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class TradeCancelImageCustomRepositoryImpl implements TradeCancelImageCustomRepository {

    private static final QTradeCancelImageJpaEntity tradeCancelImage = QTradeCancelImageJpaEntity.tradeCancelImageJpaEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<TradeCancelImageJpaEntity> findByTradeCancelIn(List<Long> tradeCancelIds) {
        return queryFactory
                .selectFrom(tradeCancelImage)
                .join(tradeCancelImage.image).fetchJoin()
                .where(tradeCancelImage.tradeCancel.id.in(tradeCancelIds))
                .fetch();
    }
}
