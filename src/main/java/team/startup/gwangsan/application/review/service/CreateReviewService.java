package team.startup.gwangsan.application.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.review.data.CreateReviewCommand;
import team.startup.gwangsan.application.review.data.ReviewTrade;
import team.startup.gwangsan.application.review.port.in.CreateReviewUseCase;
import team.startup.gwangsan.application.review.port.out.*;
import team.startup.gwangsan.domain.review.exception.AlreadyReviewedException;
import team.startup.gwangsan.domain.review.exception.CannotReviewBeforeTradeException;
import team.startup.gwangsan.domain.review.model.Review;

@Service
@RequiredArgsConstructor
public class CreateReviewService implements CreateReviewUseCase {

    private final ReviewPort reviewPort;
    private final ReviewProductPort reviewProductPort;
    private final ReviewMemberPort reviewMemberPort;
    private final ReviewBlockPort reviewBlockPort;
    private final ReviewTradePort reviewTradePort;
    private final ReviewAlertPort reviewAlertPort;

    @Override
    @Transactional
    public void execute(CreateReviewCommand command) {
        long reviewerId = command.actorId();
        long reviewedId = command.otherMemberId();
        long productId = command.productId();

        Review.validateNotSelf(reviewerId, reviewedId);

        if (!reviewProductPort.isCompleted(productId)) {
            throw new CannotReviewBeforeTradeException();
        }

        reviewMemberPort.findNickname(reviewedId);
        reviewBlockPort.validateNotBlocked(reviewerId, reviewedId);

        ReviewTrade trade = reviewTradePort.findCompletedTrade(productId)
                .orElseThrow(CannotReviewBeforeTradeException::new);
        Review.validateParticipants(reviewerId, reviewedId, trade.buyerId(), trade.sellerId());

        if (reviewPort.exists(productId, reviewerId)) {
            throw new AlreadyReviewedException();
        }

        long reviewId = reviewPort.save(new Review(null, productId, reviewerId, reviewedId, command.content(), command.light()));
        reviewMemberPort.addLight(reviewedId, command.light());
        reviewAlertPort.notifyReviewed(reviewId, reviewedId);
    }
}
