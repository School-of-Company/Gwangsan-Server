package team.startup.gwangsan.adapter.web.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.adapter.web.admin.dto.request.AnswerFeedbackRequest;
import team.startup.gwangsan.adapter.web.admin.dto.request.ChangeFeedbackStatusRequest;
import team.startup.gwangsan.adapter.web.admin.dto.response.AdminFeedbackResponse;
import team.startup.gwangsan.application.feedback.data.AdminFeedbackSearch;
import team.startup.gwangsan.application.feedback.data.AnswerFeedbackCommand;
import team.startup.gwangsan.application.feedback.data.ChangeFeedbackStatusCommand;
import team.startup.gwangsan.application.feedback.port.in.AnswerFeedbackUseCase;
import team.startup.gwangsan.application.feedback.port.in.ChangeFeedbackStatusUseCase;
import team.startup.gwangsan.application.feedback.port.in.FindAdminFeedbackQuery;
import team.startup.gwangsan.application.feedback.port.in.FindAdminFeedbacksQuery;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;

import java.util.List;

@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FindAdminFeedbacksQuery findAdminFeedbacksQuery;
    private final FindAdminFeedbackQuery findAdminFeedbackQuery;
    private final AnswerFeedbackUseCase answerFeedbackUseCase;
    private final ChangeFeedbackStatusUseCase changeFeedbackStatusUseCase;

    @GetMapping
    public ResponseEntity<List<AdminFeedbackResponse>> findFeedbacks(
            ActorContext actor,
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(required = false) FeedbackType type,
            @RequestParam(name = "place_id", required = false) Integer placeId,
            @RequestParam(name = "last_id", required = false) Long lastId,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (size < 1 || (lastId != null && lastId < 1)) {
            return ResponseEntity.badRequest().build();
        }
        List<AdminFeedbackResponse> responses = findAdminFeedbacksQuery
                .query(new AdminFeedbackSearch(actor, status, type, placeId, lastId, size)).stream()
                .map(AdminFeedbackResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{feedback_id}")
    public ResponseEntity<AdminFeedbackResponse> findFeedback(ActorContext actor,
                                                               @PathVariable("feedback_id") Long feedbackId) {
        return ResponseEntity.ok(AdminFeedbackResponse.from(findAdminFeedbackQuery.query(actor, feedbackId)));
    }

    @PatchMapping("/{feedback_id}/answer")
    public ResponseEntity<Void> answerFeedback(ActorContext actor, @PathVariable("feedback_id") Long feedbackId,
                                                @RequestBody @Valid AnswerFeedbackRequest request) {
        answerFeedbackUseCase.execute(new AnswerFeedbackCommand(actor, feedbackId, request.answer()));
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{feedback_id}/status")
    public ResponseEntity<Void> changeFeedbackStatus(ActorContext actor, @PathVariable("feedback_id") Long feedbackId,
                                                      @RequestBody @Valid ChangeFeedbackStatusRequest request) {
        changeFeedbackStatusUseCase.execute(new ChangeFeedbackStatusCommand(actor, feedbackId, request.status()));
        return ResponseEntity.ok().build();
    }
}
