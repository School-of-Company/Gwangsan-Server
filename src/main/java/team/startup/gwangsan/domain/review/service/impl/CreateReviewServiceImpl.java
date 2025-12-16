package team.startup.gwangsan.domain.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.exception.AlreadyReviewedException;
import team.startup.gwangsan.domain.review.exception.CannotReviewBeforeTradeException;
import team.startup.gwangsan.domain.review.presentation.dto.request.CreateReviewRequest;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.review.service.CreateReviewService;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class CreateReviewServiceImpl implements CreateReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberUtil memberUtil;
    private final ProductRepository productRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "receivedReviews", key = "#result")
    public Long execute(CreateReviewRequest request) {
        Member reviewer = memberUtil.getCurrentMember();

        Product product = productRepository.findById(request.productId())
                .orElseThrow(NotFoundProductException::new);

        if (product.getStatus() != ProductStatus.COMPLETED) {
            throw new CannotReviewBeforeTradeException();
        }

        if (reviewRepository.existsByProductAndReviewer(product, reviewer)) {
            throw new AlreadyReviewedException();
        }

        MemberDetail reviewerDetail = memberDetailRepository.findByMember(reviewer)
                .orElseThrow(NotFoundMemberDetailException::new);

        int rawLight = request.light();
        int light = Math.max(1, rawLight / 10);

        Review review = Review.builder()
                .product(product)
                .reviewer(reviewer)
                .reviewed(product.getMember())
                .content(request.content())
                .light(light)
                .build();

        reviewRepository.save(review);

        applicationEventPublisher.publishEvent(new CreateAlertEvent(
                review.getId(),
                product.getMember().getId(),
                AlertType.REVIEW
        ));

        return review.getReviewed().getId();
    }
}
