package team.startup.gwangsan.application.review.data;

import java.util.List;

public record ReviewDetail(Long reviewId, Long productId, String productTitle, String content, Integer light,
                           List<ReviewImage> images) {
}
