package team.startup.gwangsan.application.feedback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.feedback.port.out.AdminPlaceLookupPort;
import team.startup.gwangsan.domain.admin.exception.AdminPlaceMismatchException;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;

/**
 * ROLE_HEAD_ADMIN 은 모든 문의를 본다. ROLE_PLACE_ADMIN 은 BUG 유형은 지점과 무관하게,
 * 나머지 유형은 자기 지점 회원의 문의만 볼 수 있다.
 */
@Component
@RequiredArgsConstructor
class FeedbackAdminAccessGuard {

    private final AdminPlaceLookupPort adminPlaceLookupPort;

    void validate(ActorContext actor, FeedbackType type, Integer authorPlaceId) {
        if (actor.role() == ActorContext.Role.ROLE_HEAD_ADMIN || type == FeedbackType.BUG) {
            return;
        }
        Integer ownPlaceId = adminPlaceLookupPort.ownPlaceId(actor.memberId());
        if (!ownPlaceId.equals(authorPlaceId)) {
            throw new AdminPlaceMismatchException();
        }
    }
}
