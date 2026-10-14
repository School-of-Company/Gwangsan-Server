package team.startup.gwangsan.application.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.review.data.*;
import team.startup.gwangsan.application.review.port.in.ReviewQuery;
import team.startup.gwangsan.application.review.port.out.ReviewMemberPort;
import team.startup.gwangsan.application.review.port.out.ReviewPort;
import team.startup.gwangsan.application.review.port.out.ReviewProductPort;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewQueryService implements ReviewQuery {

    private final ReviewPort reviewPort;
    private final ReviewProductPort reviewProductPort;
    private final ReviewMemberPort reviewMemberPort;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewItem> findWritten(long actorId) {
        String reviewerName = reviewMemberPort.findNickname(actorId);
        return toItems(reviewPort.findWritten(actorId), true, reviewerName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewItem> findReceived(long actorId, ReviewPage page) {
        String targetName = reviewMemberPort.findNickname(actorId);
        return toItems(reviewPort.findReceived(actorId, page), false, targetName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewItem> findByMember(long memberId, ReviewPage page) {
        String targetName = reviewMemberPort.findNickname(memberId);
        return toItems(reviewPort.findReceived(memberId, page), false, targetName);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDetail findDetail(long reviewId) {
        ReviewDetail detail = reviewPort.findDetail(reviewId).orElseThrow(NotFoundReviewException::new);
        return new ReviewDetail(detail.reviewId(), detail.productId(), detail.productTitle(), detail.content(),
                detail.light(), reviewProductPort.findImages(detail.productId()));
    }

    /** written 이면 owner 가 작성자, 아니면 owner 가 대상이다. */
    private List<ReviewItem> toItems(List<ReviewRow> rows, boolean written, String ownerName) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = rows.stream().map(ReviewRow::productId).distinct().toList();
        Map<Long, List<ReviewImage>> images = reviewProductPort.findImages(productIds);
        return rows.stream()
                .map(r -> new ReviewItem(r.reviewId(), r.productId(), r.content(), r.light(),
                        written ? ownerName : r.otherNickname(),
                        written ? r.otherNickname() : ownerName,
                        images.getOrDefault(r.productId(), List.of())))
                .toList();
    }
}
