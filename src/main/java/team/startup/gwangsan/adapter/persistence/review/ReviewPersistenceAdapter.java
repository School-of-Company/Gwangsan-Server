package team.startup.gwangsan.adapter.persistence.review;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.review.data.ReviewAuthor;
import team.startup.gwangsan.application.review.data.ReviewDetail;
import team.startup.gwangsan.application.review.data.ReviewPage;
import team.startup.gwangsan.application.review.data.ReviewRow;
import team.startup.gwangsan.application.review.port.out.ReviewPort;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.review.model.Review;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReviewPersistenceAdapter implements ReviewPort {

    private final ReviewJpaRepository reviewJpaRepository;
    private final EntityManager entityManager;

    @Override
    public boolean exists(long productId, long reviewerId) {
        return reviewJpaRepository.existsByProductAndReviewer(product(productId), member(reviewerId));
    }

    @Override
    public long save(Review review) {
        return reviewJpaRepository.save(ReviewJpaEntity.builder()
                        .product(product(review.productId()))
                        .reviewer(member(review.reviewerId()))
                        .reviewed(member(review.reviewedId()))
                        .content(review.content())
                        .light(review.light())
                        .build())
                .getId();
    }

    @Override
    public Optional<ReviewAuthor> findAuthor(long reviewId) {
        return reviewJpaRepository.findById(reviewId)
                .map(r -> new ReviewAuthor(r.getId(), r.getReviewer().getId(), r.getReviewer().getNickname()));
    }

    @Override
    public void reassignReviewer(Long memberId, Long dummyMemberId) {
        reviewJpaRepository.reassignReviewer(member(memberId), member(dummyMemberId));
    }

    @Override
    public void reassignReviewed(Long memberId, Long dummyMemberId) {
        reviewJpaRepository.reassignReviewed(member(memberId), member(dummyMemberId));
    }

    @Override
    public List<ReviewRow> findWritten(long reviewerId) {
        return reviewJpaRepository.findMyReviews(reviewerId).stream()
                .map(r -> new ReviewRow(r.reviewId(), r.productId(), r.content(), r.light(), r.reviewedNickname()))
                .toList();
    }

    @Override
    public List<ReviewRow> findReceived(long reviewedId, ReviewPage page) {
        return reviewJpaRepository.findReceivedReviews(reviewedId, page.cursor(), page.size()).stream()
                .map(r -> new ReviewRow(r.reviewId(), r.productId(), r.content(), r.light(), r.reviewerNickname()))
                .toList();
    }

    @Override
    public Optional<ReviewDetail> findDetail(long reviewId) {
        return reviewJpaRepository.findById(reviewId)
                .map(r -> new ReviewDetail(r.getId(), r.getProduct().getId(), r.getProduct().getTitle(),
                        r.getContent(), r.getLight(), List.of()));
    }

    private Product product(long productId) {
        return entityManager.getReference(Product.class, productId);
    }

    private MemberJpaEntity member(long memberId) {
        return entityManager.getReference(MemberJpaEntity.class, memberId);
    }
}
