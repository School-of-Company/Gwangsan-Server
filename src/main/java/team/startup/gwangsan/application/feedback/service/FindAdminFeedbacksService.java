package team.startup.gwangsan.application.feedback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.feedback.data.AdminFeedbackSearch;
import team.startup.gwangsan.application.feedback.data.FeedbackDetail;
import team.startup.gwangsan.application.feedback.port.in.FindAdminFeedbackQuery;
import team.startup.gwangsan.application.feedback.port.in.FindAdminFeedbacksQuery;
import team.startup.gwangsan.application.feedback.port.out.AdminPlaceLookupPort;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort.FeedbackAccess;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.exception.NotFoundFeedbackException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindAdminFeedbacksService implements FindAdminFeedbacksQuery, FindAdminFeedbackQuery {

    private final LoadFeedbackPort loadFeedbackPort;
    private final AdminPlaceLookupPort adminPlaceLookupPort;
    private final FeedbackAdminAccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackDetail> query(AdminFeedbackSearch search) {
        ActorContext actor = search.actor();
        Integer strictPlaceId = null;
        Integer scopePlaceIdExemptingBug = null;
        if (actor.role() == ActorContext.Role.ROLE_PLACE_ADMIN) {
            scopePlaceIdExemptingBug = adminPlaceLookupPort.ownPlaceId(actor.memberId());
        } else if (search.placeId() != null) {
            strictPlaceId = adminPlaceLookupPort.requirePlace(search.placeId());
        }
        return loadFeedbackPort.findAdminDetails(search.status(), search.type(), strictPlaceId,
                scopePlaceIdExemptingBug, search.lastId(), search.size());
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackDetail query(ActorContext actor, Long feedbackId) {
        FeedbackAccess access = loadFeedbackPort.findAccess(feedbackId).orElseThrow(NotFoundFeedbackException::new);
        accessGuard.validate(actor, access.type(), access.placeId());
        return loadFeedbackPort.findDetail(feedbackId).orElseThrow(NotFoundFeedbackException::new);
    }
}
