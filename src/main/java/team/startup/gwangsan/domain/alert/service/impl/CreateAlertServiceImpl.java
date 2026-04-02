package team.startup.gwangsan.domain.alert.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.exception.NotFoundAlertTypeException;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.entity.AlertReceipt;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.alert.repository.AlertReceiptRepository;
import team.startup.gwangsan.domain.alert.repository.AlertRepository;
import team.startup.gwangsan.domain.alert.service.CreateAlertService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.suspend.entity.Suspend;
import team.startup.gwangsan.domain.suspend.exception.NotFoundSuspendException;
import team.startup.gwangsan.domain.suspend.repository.SuspendRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;

import static team.startup.gwangsan.domain.admin.service.impl.VerificationSignUpServiceImpl.RECOMMENDER_GWANGSAN_REWARD;

@Service
@RequiredArgsConstructor
public class CreateAlertServiceImpl implements CreateAlertService {

    private final AlertRepository alertRepository;
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final TradeCompleteRepository tradeCompleteRepository;
    private final AlertReceiptRepository alertReceiptRepository;
    private final ReportRepository reportRepository;
    private final TradeCancelRepository tradeCancelRepository;
    private final SuspendRepository suspendRepository;

    private static final String TRADE_COMPLETE_REJECT_CONTENT = "거래 완료가 거절되었습니다.";
    private static final String OTHER_MEMBER_TRADE_COMPLETE_CONTENT = "님이 거래를 완료하였습니다.";
    private static final String RECOMMENDER_TITLE = "추천인 등록";
    private static final String RECOMMENDER_CONTENT = "추천인으로 등록되어 %d 광산이 지급되었습니다.";
    private static final String REVIEW_TITLE = "리뷰 등록";
    private static final String REVIEW_CONTENT = "나에 대한 후기가 등록되었습니다.";
    private static final String REPORT_REJECT_TITLE = "신고 기각";
    private static final String REPORT_REJECT_CONTENT = "님에 대한 신고가 기각되었습니다.";
    private static final String TRADE_CANCEL_REJECT_TITLE = "거래 철회 기각";
    private static final String TRADE_CANCEL_REJECT_CONTENT = "거래 철회 요청이 기각되었습니다.";
    private static final String TRADE_CANCEL_TITLE = "거래 철회 완료";
    private static final String TRADE_CANCEL_CONTENT = "거래가 철회되었습니다.";
    private static final String REPORT_TITLE = "신고 결과";
    private static final String REPORT_CONTENT = "일 정지 조치되었습니다.";

    private static final String BUYER_TRADE_BODY = "광산이 차감되었습니다.";
    private static final String SELLER_TRADE_BODY = "광산이 추가되었습니다.";

    @Override
    @Transactional
    public void execute(Long sourceId, Long memberId, AlertType alertType, Long suspendId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        switch (alertType) {
            case TRADE_COMPLETE -> {
                TradeComplete tradeComplete = tradeCompleteRepository.findById(sourceId)
                        .orElseThrow(NotFoundTradeCompleteException::new);

                Product product = tradeComplete.getProduct();
                Member buyer = tradeComplete.getBuyer();
                Member seller = tradeComplete.getSeller();

                Alert buyerAlert = saveAlert(
                        product.getId(),
                        alertType,
                        seller,
                        product.getTitle(),
                        BUYER_TRADE_BODY
                );
                saveAlertReceipt(buyerAlert, buyer);

                Alert sellerAlert = saveAlert(
                        product.getId(),
                        alertType,
                        buyer,
                        product.getTitle(),
                        SELLER_TRADE_BODY
                );
                saveAlertReceipt(sellerAlert, seller);
            }

            case TRADE_COMPLETE_REJECT -> {
                Product product = getProduct(sourceId);
                Alert alert = saveAlert(sourceId, alertType, product.getTitle(), TRADE_COMPLETE_REJECT_CONTENT);
                saveAlertReceipt(alert, member);
            }

            case OTHER_MEMBER_TRADE_COMPLETE -> {
                TradeComplete tradeComplete = tradeCompleteRepository.findById(sourceId)
                        .orElseThrow(NotFoundTradeCompleteException::new);
                Alert alert = saveAlert(
                        tradeComplete.getProduct().getId(),
                        alertType,
                        tradeComplete.getSeller(),
                        tradeComplete.getProduct().getTitle(),
                        tradeComplete.getBuyer().getNickname() + OTHER_MEMBER_TRADE_COMPLETE_CONTENT
                );
                saveAlertReceipt(alert, member);
            }

            case RECOMMENDER -> {
                Member signUpMember = getMember(sourceId);
                Alert alert = saveAlert(
                        sourceId,
                        alertType,
                        signUpMember,
                        RECOMMENDER_TITLE,
                        String.format(RECOMMENDER_CONTENT, RECOMMENDER_GWANGSAN_REWARD));
                saveAlertReceipt(alert, member);
            }

            case REVIEW -> {
                Review review = reviewRepository.findById(sourceId)
                        .orElseThrow(NotFoundReviewException::new);
                Alert alert = saveAlert(
                        sourceId,
                        alertType,
                        review.getReviewer(),
                        review.getReviewer().getNickname() + REVIEW_TITLE,
                        REVIEW_CONTENT
                );
                saveAlertReceipt(alert, member);
            }

            case REPORT_REJECT -> {
                Report report = reportRepository.findById(sourceId)
                        .orElseThrow(NotFoundReportException::new);

                Alert alert = saveAlert(
                        sourceId,
                        alertType,
                        report.getReporter(),
                        REPORT_REJECT_TITLE,
                        report.getReported().getNickname() + REPORT_REJECT_CONTENT
                );
                saveAlertReceipt(alert, member);
            }

            case TRADE_CANCEL_REJECT -> {
                TradeCancel tradeCancel = tradeCancelRepository.findById(sourceId)
                        .orElseThrow(NotFoundTradeCancelException::new);
                Alert alert = saveAlert(
                        sourceId,
                        alertType,
                        tradeCancel.getMember(),
                        TRADE_CANCEL_REJECT_TITLE,
                        TRADE_CANCEL_REJECT_CONTENT
                );
                saveAlertReceipt(alert, member);
            }

            case TRADE_CANCEL -> {
                TradeCancel tradeCancel = tradeCancelRepository.findByIdWithTradeCompleteAndMember(sourceId)
                        .orElseThrow(NotFoundTradeCancelException::new);

                Member buyerMember = tradeCancel.getTradeComplete().getBuyer();
                Member sellerMember = tradeCancel.getTradeComplete().getSeller();

                Alert alert = saveAlert(
                        sourceId,
                        alertType,
                        tradeCancel.getMember(),
                        TRADE_CANCEL_TITLE,
                        TRADE_CANCEL_CONTENT
                );
                saveAlertReceipt(alert, buyerMember);
                saveAlertReceipt(alert, sellerMember);
            }

            case REPORT -> {
                Report report = reportRepository.findById(sourceId)
                        .orElseThrow(NotFoundReportException::new);

                Suspend suspend = suspendRepository.findById(suspendId)
                        .orElseThrow(NotFoundSuspendException::new);

                Alert alert = saveAlert(
                        sourceId,
                        alertType,
                        report.getReporter(),
                        REPORT_TITLE,
                        suspend.getSuspendedDays() + REPORT_CONTENT
                );
                saveAlertReceipt(alert, member);
            }

            default -> throw new NotFoundAlertTypeException();
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

    private Alert createAlert(Long sourceId, AlertType alertType, String title, String content) {
        return Alert.builder()
                .alertType(alertType)
                .sourceId(sourceId)
                .title(title)
                .content(content)
                .build();
    }

    private Alert createAlert(Long sourceId, AlertType alertType, Member sendMember, String title, String content) {
        return Alert.builder()
                .alertType(alertType)
                .sourceId(sourceId)
                .sendMember(sendMember)
                .title(title)
                .content(content)
                .build();
    }

    private AlertReceipt createAlertReceipt(Alert alert, Member member) {
        return AlertReceipt.builder()
                .alert(alert)
                .checked(false)
                .member(member)
                .build();
    }

    private void saveAlertReceipt(Alert alert, Member member) {
        alertReceiptRepository.save(createAlertReceipt(alert, member));
    }

    private Alert saveAlert(Long sourceId, AlertType alertType, String title, String content) {
        return alertRepository.save(createAlert(sourceId, alertType, title, content));
    }

    private Alert saveAlert(Long sourceId, AlertType alertType, Member sendMember, String title, String content) {
        return alertRepository.save(createAlert(sourceId, alertType, sendMember, title, content));
    }
}