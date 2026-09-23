package team.startup.gwangsan.domain.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.exception.AlreadyReviewedException;
import team.startup.gwangsan.domain.review.exception.CannotReviewBeforeTradeException;
import team.startup.gwangsan.domain.review.exception.CannotReviewSelfException;
import team.startup.gwangsan.domain.review.exception.NotTradeParticipantException;
import team.startup.gwangsan.domain.review.presentation.dto.request.CreateReviewRequest;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.review.service.CreateReviewService;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.util.BlockValidator;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class CreateReviewServiceImpl implements CreateReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberUtil memberUtil;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final TradeCompleteRepository tradeCompleteRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final BlockValidator blockValidator;

    @Override
    @Transactional
    public void execute(CreateReviewRequest request) {
        Member reviewer = memberUtil.getCurrentMember();

        if (reviewer.getId().equals(request.otherMemberId())) {
            throw new CannotReviewSelfException();
        }

        Product product = productRepository.findActiveById(request.productId())
                .orElseThrow(NotFoundProductException::new);

        if (product.getStatus() != ProductStatus.COMPLETED) {
            throw new CannotReviewBeforeTradeException();
        }

        Member reviewed = memberRepository.findById(request.otherMemberId())
                .orElseThrow(NotFoundMemberException::new);

        blockValidator.validate(reviewer, reviewed);

        TradeComplete completedTrade = tradeCompleteRepository
                .findByProductAndStatus(product, TradeStatus.COMPLETED)
                .orElseThrow(CannotReviewBeforeTradeException::new);

        Long buyerId = completedTrade.getBuyer().getId();
        Long sellerId = completedTrade.getSeller().getId();
        boolean isTradeParticipants =
                (buyerId.equals(reviewer.getId()) && sellerId.equals(reviewed.getId()))
                        || (sellerId.equals(reviewer.getId()) && buyerId.equals(reviewed.getId()));

        if (!isTradeParticipants) {
            throw new NotTradeParticipantException();
        }

        if (reviewRepository.existsByProductAndReviewer(product, reviewer)) {
            throw new AlreadyReviewedException();
        }

        MemberDetail reviewedDetail = memberDetailRepository.findByMember(reviewed)
                .orElseThrow(NotFoundMemberDetailException::new);

        int rawLight = request.light();

        Review review = Review.builder()
                .product(product)
                .reviewer(reviewer)
                .reviewed(reviewed)
                .content(request.content())
                .light(rawLight)
                .build();

        reviewRepository.save(review);

        reviewedDetail.plusLight(rawLight);

        applicationEventPublisher.publishEvent(new CreateAlertEvent(
                review.getId(),
                reviewed.getId(),
                AlertType.REVIEW
        ));

    }
}
