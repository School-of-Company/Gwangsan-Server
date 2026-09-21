package team.startup.gwangsan.domain.review.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.review.presentation.dto.request.CreateReviewRequest;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewDetailResponse;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;
import team.startup.gwangsan.domain.review.service.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final CreateReviewService createReviewService;
    private final GetMyReviewListService getMyReviewListService;
    private final GetReceivedReviewListService getReceivedReviewListService;
    private final GetReviewByMemberService getReviewByMemberService;
    private final GetReviewDetailService getReviewDetailService;

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
    public ResponseEntity<List<ReviewResponse>> getMyReceivedReviews(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size) {
        if (!validPage(cursor, size)) return ResponseEntity.badRequest().build();
        List<ReviewResponse> response = size == null
                ? getReceivedReviewListService.execute()
                : getReceivedReviewListService.execute(cursor, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<List<ReviewResponse>> getReviewByMember(
            @PathVariable Long memberId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size) {
        if (!validPage(cursor, size)) return ResponseEntity.badRequest().build();
        List<ReviewResponse> response = size == null
                ? getReviewByMemberService.execute(memberId)
                : getReviewByMemberService.execute(memberId, cursor, size);
        return ResponseEntity.ok(response);
    }

    @InitBinder
    public void bindNumbers(WebDataBinder binder) {
        binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, false));
        binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, false));
    }

    private boolean validPage(Long cursor, Integer size) {
        return (size == null ? cursor == null : size >= 1 && size <= 100)
                && (cursor == null || cursor > 0);
    }

    @GetMapping("/detail/{reviewId}")
    public ResponseEntity<ReviewDetailResponse> getReviewDetail(@PathVariable Long reviewId) {
        ReviewDetailResponse response = getReviewDetailService.execute(reviewId);
        return ResponseEntity.ok(response);
    }
}
