package team.startup.gwangsan.application.feedback.port.out;

import team.startup.gwangsan.application.feedback.data.FeedbackDetail;
import team.startup.gwangsan.application.feedback.data.FeedbackSummary;
import team.startup.gwangsan.domain.feedback.model.Feedback;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;

import java.util.List;
import java.util.Optional;

public interface LoadFeedbackPort {

    Optional<Feedback> findById(Long feedbackId);

    List<FeedbackSummary> findMySummaries(Long memberId, Long lastId, int size);

    Optional<FeedbackDetail> findDetail(Long feedbackId);

    /** 관리자 범위 검증용 최소 조회(유형 + 작성자 지점). */
    Optional<FeedbackAccess> findAccess(Long feedbackId);

    record FeedbackAccess(FeedbackType type, Integer placeId) {
    }

    /**
     * 관리자 목록 조회. strictPlaceId 가 있으면 유형과 무관하게 그 지점만(HEAD_ADMIN 의 명시적 필터),
     * scopePlaceIdExemptingBug 가 있으면 BUG 는 전체·나머지 유형은 그 지점만(PLACE_ADMIN 권한 범위) 본다.
     * 둘 다 null 이면 제한 없이 전체를 본다.
     */
    List<FeedbackDetail> findAdminDetails(FeedbackStatus status, FeedbackType type, Integer strictPlaceId,
                                           Integer scopePlaceIdExemptingBug, Long lastId, int size);
}
