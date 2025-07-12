package team.startup.gwangsan.domain.post.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.post.entity.QTradeComplete;
import team.startup.gwangsan.domain.post.repository.custom.TradeCompleteCustomRepository;

@Repository
@RequiredArgsConstructor
public class TradeCompleteCustomRepositoryImpl implements TradeCompleteCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public long countMutualTradeComplete(Long productId, Long memberId1, Long memberId2) {
        QTradeComplete tradeComplete = QTradeComplete.tradeComplete;

        Long count = queryFactory
                .select(tradeComplete.count())
                .from(tradeComplete)
                .where(
                        tradeComplete.product.id.eq(productId),
                        tradeComplete.member.id.in(memberId1, memberId2),
                        tradeComplete.otherMember.id.in(memberId1, memberId2)
                )
                .fetchOne();

        return count != null ? count : 0L;
    }
}
