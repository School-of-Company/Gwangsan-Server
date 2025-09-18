package team.startup.gwangsan.domain.alert.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.alert.repository.AlertRepository;
import team.startup.gwangsan.domain.alert.service.CreateAlertService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.TradeComplete;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;

@Service
@RequiredArgsConstructor
public class CreateAlertServiceImpl implements CreateAlertService {

    private final AlertRepository alertRepository;
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final TradeCompleteRepository tradeCompleteRepository;

    private static final String TRADE_COMPLETE_CONTENT = "거래가 완료되었습니다.";
    private static final String TRADE_COMPLETE_REJECT_CONTENT = "거래 완료가 거절되었습니다.";
    private static final String OTHER_MEMBER_TRADE_COMPLETE_CONTENT = "님이 거래를 완료하였습니다.";
    private static final String RECOMMENDER_TITLE = "추천인 등록";
    private static final String RECOMMENDER_CONTENT = "추천인으로 등록되었습니다.";
    private static final String REVIEW_TITLE = "리뷰 등록";
    private static final String REVIEW_CONTENT = "나에 대한 후기가 등록되었습니다.";

    @Override
    @Transactional
    public void execute(Long sourceId, Long memberId, AlertType alertType) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        switch (alertType) {
            case TRADE_COMPLETE -> {
                Product product = getProduct(sourceId);

                saveAlert(sourceId, alertType, member, product.getTitle(), TRADE_COMPLETE_CONTENT);
            }

            case TRADE_COMPLETE_REJECT -> {
                Product product = getProduct(sourceId);

                saveAlert(sourceId, alertType, member, product.getTitle(), TRADE_COMPLETE_REJECT_CONTENT);
            }

            case OTHER_MEMBER_TRADE_COMPLETE -> {
                TradeComplete tradeComplete = tradeCompleteRepository.findById(sourceId)
                        .orElseThrow(NotFoundTradeCompleteException::new);

                saveAlert(tradeComplete.getProduct().getId(), alertType, member, tradeComplete.getBuyer(), tradeComplete.getProduct().getTitle(), tradeComplete.getBuyer().getNickname() + OTHER_MEMBER_TRADE_COMPLETE_CONTENT);
            }

            case RECOMMENDER -> {
                Member signUpMember = getMember(sourceId);

                saveAlert(sourceId, alertType, member, signUpMember, RECOMMENDER_TITLE, RECOMMENDER_CONTENT);
            }

            case REVIEW -> {
                Review review = reviewRepository.findById(sourceId)
                        .orElseThrow(NotFoundReviewException::new);

                saveAlert(sourceId, alertType, member, review.getReviewer().getNickname() + REVIEW_TITLE, REVIEW_CONTENT);
            }
        }
    }

    private Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(NotFoundMemberException::new);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(NotFoundProductException::new);
    }


    private Alert createAlert(Long sourceId, AlertType alertType, Member member, String title, String content) {
        return Alert.builder()
                .alertType(alertType)
                .sourceId(sourceId)
                .member(member)
                .title(title)
                .content(content)
                .checked(false)
                .build();
    }

    private Alert createAlert(Long sourceId, AlertType alertType, Member member, Member sendMember, String title, String content) {
        return Alert.builder()
                .alertType(alertType)
                .sourceId(sourceId)
                .member(member)
                .sendMember(sendMember)
                .title(title)
                .content(content)
                .checked(false)
                .build();
    }

    private void saveAlert(Long sourceId, AlertType alertType, Member member, String title, String content) {
        alertRepository.save(createAlert(sourceId, alertType, member, title, content));
    }

    private void saveAlert(Long sourceId, AlertType alertType, Member member, Member sendMember, String title, String content) {
        alertRepository.save(createAlert(sourceId, alertType, member, sendMember, title, content));
    }
}
