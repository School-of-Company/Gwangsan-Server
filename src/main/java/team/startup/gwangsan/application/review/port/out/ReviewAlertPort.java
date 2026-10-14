package team.startup.gwangsan.application.review.port.out;

/** 후기 등록 알림(기존 커밋 후 비동기 저장 유지). */
public interface ReviewAlertPort {
    void notifyReviewed(long reviewId, long reviewedId);
}
