package team.startup.gwangsan.application.review.data;

/** 후기 한 건과 상대방(작성한 후기면 대상, 받은 후기면 작성자) 별명. */
public record ReviewRow(Long reviewId, Long productId, String content, Integer light, String otherNickname) {
}
