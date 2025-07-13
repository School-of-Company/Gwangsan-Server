package team.startup.gwangsan.domain.review.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.review.presentation.dto.request.CreateReviewRequest;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;
import team.startup.gwangsan.domain.review.service.CreateReviewService;
import team.startup.gwangsan.domain.review.service.GetMyReviewListService;
import team.startup.gwangsan.domain.review.service.GetReceivedReviewListService;
import team.startup.gwangsan.domain.review.service.GetReviewByMemberService;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final CreateReviewService createReviewService;
    private final GetMyReviewListService getMyReviewListService;
    private final GetReceivedReviewListService getReceivedReviewListService;
    private final GetReviewByMemberService getReviewByMemberService;

    @PostMapping
    public ResponseEntity<Void> createReview(@RequestBody @Valid CreateReviewRequest request) {
        createReviewService.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getMyReviews() {
        List<ReviewResponse> responses = getMyReviewListService.execute();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/current")
    public ResponseEntity<List<ReviewResponse>> getMyReceivedReviews() {
        List<ReviewResponse> response = getReceivedReviewListService.execute();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<List<ReviewResponse>> getReviewByMember(
            @PathVariable Long memberId) {
        List<ReviewResponse> response = getReviewByMemberService.execute(memberId);
        return ResponseEntity.ok(response);
    }

}
