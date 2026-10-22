package team.startup.gwangsan.application.feedback.data;

import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.time.LocalDateTime;
import java.util.List;

/** 관리자 조회용 문의 상세. placeId 는 PLACE_ADMIN 범위 검증에, placeName 은 응답 표시에 쓴다. */
public record FeedbackDetail(
        Long feedbackId,
        Long memberId,
        String nickname,
        Integer placeId,
        String placeName,
        FeedbackType type,
        String content,
        List<FeedbackImageLink> images,
        String appVersion,
        OsType osType,
        String osVersion,
        String deviceModel,
        FeedbackStatus status,
        String answer,
        LocalDateTime answeredAt,
        String answeredByNickname,
        LocalDateTime createdAt
) {
}
