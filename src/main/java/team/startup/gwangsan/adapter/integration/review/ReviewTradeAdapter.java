package team.startup.gwangsan.adapter.integration.review;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.review.data.ReviewTrade;
import team.startup.gwangsan.application.review.port.out.ReviewTradePort;
import team.startup.gwangsan.application.trade.port.in.CompletedTradeQuery;

import java.util.Optional;

/** review → trade 공개 query 연결. */
@Component
@RequiredArgsConstructor
public class ReviewTradeAdapter implements ReviewTradePort {

    private final CompletedTradeQuery completedTradeQuery;

    @Override
    public Optional<ReviewTrade> findCompletedTrade(long productId) {
        return completedTradeQuery.query(productId).map(t -> new ReviewTrade(t.buyerId(), t.sellerId()));
    }
}
