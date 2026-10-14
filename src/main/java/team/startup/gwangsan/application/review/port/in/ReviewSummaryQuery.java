package team.startup.gwangsan.application.review.port.in;

import team.startup.gwangsan.application.review.data.ReviewAuthor;

/** 후기 요약 공개 조회(알림 원천 등). */
public interface ReviewSummaryQuery {
    /** 없으면 NotFoundReviewException. 기존 경로와 같이 후기 1회 + 작성자 지연 로딩. */
    ReviewAuthor getAuthor(long reviewId);
}
