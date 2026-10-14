package team.startup.gwangsan.application.review.data;

import java.util.List;

public record ReviewItem(Long reviewId, Long productId, String content, Integer light,
                         String reviewerName, String targetName, List<ReviewImage> images) {
}
