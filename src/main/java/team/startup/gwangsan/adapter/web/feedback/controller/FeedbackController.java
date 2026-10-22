package team.startup.gwangsan.adapter.web.feedback.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.adapter.web.feedback.dto.request.CreateFeedbackRequest;
import team.startup.gwangsan.adapter.web.feedback.dto.response.FeedbackSummaryResponse;
import team.startup.gwangsan.application.feedback.data.MyFeedbackSearch;
import team.startup.gwangsan.application.feedback.port.in.CreateFeedbackUseCase;
import team.startup.gwangsan.application.feedback.port.in.FindMyFeedbacksQuery;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final CreateFeedbackUseCase createFeedbackUseCase;
    private final FindMyFeedbacksQuery findMyFeedbacksQuery;

    @PostMapping
    public ResponseEntity<Void> createFeedback(ActorContext actor, @RequestBody @Valid CreateFeedbackRequest request) {
        createFeedbackUseCase.execute(request.toCommand(actor));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    public ResponseEntity<List<FeedbackSummaryResponse>> findMyFeedbacks(
            ActorContext actor,
            @RequestParam(name = "last_id", required = false) Long lastId,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        if (size < 1 || (lastId != null && lastId < 1)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(findMyFeedbacksQuery.query(new MyFeedbackSearch(actor, lastId, size)).stream()
                .map(FeedbackSummaryResponse::from)
                .toList());
    }
}
