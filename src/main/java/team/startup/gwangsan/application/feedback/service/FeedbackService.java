package team.startup.gwangsan.application.feedback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import team.startup.gwangsan.application.feedback.data.*;
import team.startup.gwangsan.application.feedback.port.in.*;
import team.startup.gwangsan.application.feedback.port.out.*;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.*;
import team.startup.gwangsan.domain.feedback.exception.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService implements FeedbackQuery, UpdateFeedbackUseCase, FeedbackCleanupUseCase {
    private final FeedbackPort feedbackPort;
    private final FeedbackReferencesPort references;

    @Override
    public List<FeedbackDetail> mine(Long memberId, Long lastId, int size) {
        validatePage(lastId, size);
        return feedbackPort.search(new FeedbackSearch(memberId, null, null, null, null, lastId, size));
    }

    @Override
    public List<FeedbackDetail> list(ActorContext actor, FeedbackStatus status, FeedbackType type,
                                     Integer placeId, Long lastId, int size) {
        validatePage(lastId, size);
        Integer restriction = restriction(actor);
        return feedbackPort.search(new FeedbackSearch(null, restriction, restriction == null ? placeId : null,
                status, type, lastId, size));
    }

    @Override
    public FeedbackDetail detail(ActorContext actor, Long id) {
        Integer restriction = restriction(actor);
        FeedbackDetail detail = feedbackPort.find(id).orElseThrow(NotFoundFeedbackException::new);
        if (restriction != null && detail.feedback().type() != FeedbackType.BUG
                && !Objects.equals(restriction, detail.placeId())) throw new ForbiddenFeedbackException();
        return detail;
    }

    @Override
    @Transactional
    public void answer(ActorContext actor, Long id, String answer) {
        Feedback feedback = detail(actor, id).feedback().answer(answer, actor.memberId(), LocalDateTime.now());
        feedbackPort.update(feedback);
        references.notifyAnswered(id, feedback.memberId(), actor.memberId());
    }

    @Override
    @Transactional
    public void changeStatus(ActorContext actor, Long id, FeedbackStatus status) {
        feedbackPort.update(detail(actor, id).feedback().changeStatus(status, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void reassign(Long memberId, Long dummyMemberId) {
        feedbackPort.reassign(memberId, dummyMemberId);
    }

    @Override
    @Transactional
    public void unlink(Long feedbackId, Long imageId) {
        feedbackPort.unlink(feedbackId, imageId);
    }

    private Integer restriction(ActorContext actor) {
        return switch (actor.role()) {
            case ROLE_HEAD_ADMIN -> null;
            case ROLE_PLACE_ADMIN -> references.requirePlace(actor.memberId());
            default -> throw new ForbiddenFeedbackException();
        };
    }

    private void validatePage(Long lastId, int size) {
        if (size < 1 || size > 100 || (lastId != null && lastId <= 0)) throw new InvalidFeedbackException();
    }
}
