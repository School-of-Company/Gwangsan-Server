package team.startup.gwangsan.domain.trade.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.trade.entity.TradeCancelImage;
import team.startup.gwangsan.domain.trade.repository.custom.TradeCancelImageCustomRepository;

import java.util.List;

import static team.startup.gwangsan.domain.trade.entity.QTradeCancelImage.tradeCancelImage;

@Repository
@RequiredArgsConstructor
public class TradeCancelImageCustomRepositoryImpl implements TradeCancelImageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<TradeCancelImage> findByTradeCancelIn(List<Long> tradeCancelIds) {
        return queryFactory
                .selectFrom(tradeCancelImage)
                .join(tradeCancelImage.image).fetchJoin()
                .where(tradeCancelImage.tradeCancel.id.in(tradeCancelIds))
                .fetch();
    }
}
