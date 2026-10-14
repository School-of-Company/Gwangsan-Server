package team.startup.gwangsan.application.review.port.out;

import team.startup.gwangsan.application.review.data.ReviewTrade;

import java.util.Optional;

public interface ReviewTradePort {
    Optional<ReviewTrade> findCompletedTrade(long productId);
}
