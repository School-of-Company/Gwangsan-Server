package team.startup.gwangsan.application.review.data;

/** 후기 작성자(알림 원천 등). */
public record ReviewAuthor(long reviewId, long reviewerId, String reviewerNickname) {
}
