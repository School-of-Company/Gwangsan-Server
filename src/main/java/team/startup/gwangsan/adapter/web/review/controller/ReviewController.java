package team.startup.gwangsan.adapter.web.review.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.adapter.web.review.dto.CreateReviewRequest;
import team.startup.gwangsan.adapter.web.review.dto.ReviewDetailResponse;
import team.startup.gwangsan.adapter.web.review.dto.ReviewResponse;
import team.startup.gwangsan.application.review.data.*;
import team.startup.gwangsan.application.review.port.in.CreateReviewUseCase;
import team.startup.gwangsan.application.review.port.in.ReviewQuery;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.adapter.web.image.dto.GetImageResponse;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final CreateReviewUseCase createReviewUseCase;
    private final ReviewQuery reviewQuery;

    @PostMapping
    public ResponseEntity<Void> createReview(ActorContext actor, @RequestBody @Valid CreateReviewRequest request) {
        createReviewUseCase.execute(new CreateReviewCommand(actor.memberId(), request.productId(),
                request.otherMemberId(), request.content(), request.light()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getMyReviews(ActorContext actor) {
        return ResponseEntity.ok(toResponses(reviewQuery.findWritten(actor.memberId())));
    }

    @GetMapping("/current")
    public ResponseEntity<List<ReviewResponse>> getMyReceivedReviews(
            ActorContext actor,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size) {
        if (!validPage(cursor, size)) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(toResponses(reviewQuery.findReceived(actor.memberId(), new ReviewPage(cursor, size))));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<List<ReviewResponse>> getReviewByMember(
            @PathVariable Long memberId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size) {
        if (!validPage(cursor, size)) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(toResponses(reviewQuery.findByMember(memberId, new ReviewPage(cursor, size))));
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
        ReviewDetail d = reviewQuery.findDetail(reviewId);
        return ResponseEntity.ok(new ReviewDetailResponse(d.reviewId(), d.productId(), d.productTitle(), d.content(),
                d.light(), toImages(d.images())));
    }

    private static List<ReviewResponse> toResponses(List<ReviewItem> items) {
        return items.stream()
                .map(i -> new ReviewResponse(i.reviewId(), i.productId(), i.content(), i.light(),
                        i.reviewerName(), i.targetName(), toImages(i.images())))
                .toList();
    }

    private static List<GetImageResponse> toImages(List<ReviewImage> images) {
        return images.stream().map(img -> new GetImageResponse(img.imageId(), img.imageUrl())).toList();
    }
}
