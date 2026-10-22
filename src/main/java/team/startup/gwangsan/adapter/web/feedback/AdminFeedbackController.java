package team.startup.gwangsan.adapter.web.feedback;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import team.startup.gwangsan.application.feedback.port.in.*;
import team.startup.gwangsan.adapter.web.feedback.dto.AdminFeedbackResponse;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/feedback")
public class AdminFeedbackController {
    private final FeedbackQuery feedbackQuery;
    private final UpdateFeedbackUseCase updateFeedback;

    @GetMapping
    public List<AdminFeedbackResponse> list(ActorContext actor,
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(required = false) FeedbackType type,
            @RequestParam(name = "place_id", required = false) Integer placeId,
            @RequestParam(name = "last_id", required = false) Long lastId,
            @RequestParam(defaultValue = "20") int size) {
        return feedbackQuery.list(actor, status, type, placeId, lastId, size).stream()
                .map(AdminFeedbackResponse::from).toList();
    }

    @GetMapping("/{feedback_id}")
    public AdminFeedbackResponse detail(ActorContext actor, @PathVariable("feedback_id") Long id) {
        return AdminFeedbackResponse.from(feedbackQuery.detail(actor, id));
    }

    @PatchMapping("/{feedback_id}/answer")
    public ResponseEntity<Void> answer(ActorContext actor, @PathVariable("feedback_id") Long id,
                                      @Valid @RequestBody AnswerRequest request) {
        updateFeedback.answer(actor, id, request.answer());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{feedback_id}/status")
    public ResponseEntity<Void> status(ActorContext actor, @PathVariable("feedback_id") Long id,
                                      @Valid @RequestBody StatusRequest request) {
        updateFeedback.changeStatus(actor, id, request.status());
        return ResponseEntity.noContent().build();
    }

    public record AnswerRequest(@NotBlank String answer) {}
    public record StatusRequest(@NotNull FeedbackStatus status) {}
}
