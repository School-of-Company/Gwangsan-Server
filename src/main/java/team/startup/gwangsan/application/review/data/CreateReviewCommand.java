package team.startup.gwangsan.application.review.data;

public record CreateReviewCommand(long actorId, long productId, long otherMemberId, String content, Integer light) {
}
