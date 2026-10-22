package team.startup.gwangsan.adapter.web.feedback;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import team.startup.gwangsan.application.feedback.port.in.*;
import team.startup.gwangsan.adapter.web.feedback.dto.*;
import team.startup.gwangsan.domain.common.ActorContext;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final CreateFeedbackUseCase createFeedback;
    private final FeedbackQuery feedbackQuery;

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody CreateFeedbackRequest request, ActorContext actor) {
        createFeedback.execute(request.toCommand(actor.memberId()));
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/me")
    public List<MyFeedbackResponse> mine(ActorContext actor,
            @RequestParam(name = "last_id", required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        return feedbackQuery.mine(actor.memberId(), lastId, size).stream().map(MyFeedbackResponse::from).toList();
    }
}
