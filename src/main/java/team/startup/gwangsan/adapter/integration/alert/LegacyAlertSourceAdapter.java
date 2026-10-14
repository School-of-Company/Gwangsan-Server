package team.startup.gwangsan.adapter.integration.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.suspend.exception.NotFoundSuspendException;
import team.startup.gwangsan.domain.suspend.repository.SuspendRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;

import java.util.List;

import static team.startup.gwangsan.domain.admin.service.impl.VerificationSignUpServiceImpl.RECOMMENDER_GWANGSAN_REWARD;

/**
 * 임시 bridge: 기존 CreateAlertServiceImpl 이 하던 원천 조회를 legacy Repository 로 그대로 수행한다.
 * 비동기 알림 경로는 유지되므로 Port 는 남고, 구현만 각 업무의 공개 조회로 교체한다
 * (회원 PR02, 상품 PR06, 거래·후기 PR07, 신고·정지 PR09, 공지 PR10, 최종 PR12).
 */
@Component
@RequiredArgsConstructor
public class LegacyAlertSourceAdapter implements LoadAlertSourcePort {

    private final MemberAccountQuery memberAccountQuery;
    // bridge: 회원 batch 조회 공개 Port 가 없어 N+1 을 피하려고 직접 쓴다. 스택 끝 정리 단계에서 PR02 batch Port 추가 후 교체한다.
    private final MemberJpaRepository memberJpaRepository;
    private final TradeCompleteRepository tradeCompleteRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final SuspendRepository suspendRepository;
    private final TradeCancelRepository tradeCancelRepository;
    private final NoticeRepository noticeRepository;

    @Override
    public void requireMember(Long memberId) {
        memberAccountQuery.findById(memberId).orElseThrow(NotFoundMemberException::new);
    }

    @Override
    public List<Long> existingMemberIds(List<Long> memberIds) {
        return memberJpaRepository.findAllByIdIn(memberIds).stream().map(MemberJpaEntity::getId).toList();
    }

    @Override
    public int recommenderReward() {
        return RECOMMENDER_GWANGSAN_REWARD;
    }

    @Override
    public TradeCompleteSource tradeComplete(Long tradeCompleteId) {
        TradeComplete trade = tradeCompleteRepository.findById(tradeCompleteId)
                .orElseThrow(NotFoundTradeCompleteException::new);
        return new TradeCompleteSource(
                trade.getProduct().getId(),
                trade.getProduct().getTitle(),
                trade.getBuyer().getId(),
                trade.getSeller().getId(),
                trade.getBuyer().getNickname()
        );
    }

    @Override
    public String activeProductTitle(Long productId) {
        return productRepository.findActiveById(productId).orElseThrow(NotFoundProductException::new).getTitle();
    }

    @Override
    public ReviewSource review(Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(NotFoundReviewException::new);
        return new ReviewSource(review.getReviewer().getId(), review.getReviewer().getNickname());
    }

    @Override
    public ReportSource report(Long reportId) {
        Report report = reportRepository.findById(reportId).orElseThrow(NotFoundReportException::new);
        return new ReportSource(report.getReporter().getId(), report.getReported().getNickname());
    }

    @Override
    public int suspendedDays(Long suspendId) {
        if (suspendId == null) throw new NotFoundSuspendException();
        return suspendRepository.findById(suspendId).orElseThrow(NotFoundSuspendException::new).getSuspendedDays();
    }

    @Override
    public Long tradeCancelRequester(Long tradeCancelId) {
        return tradeCancelRepository.findById(tradeCancelId)
                .orElseThrow(NotFoundTradeCancelException::new)
                .getMember().getId();
    }

    @Override
    public TradeCancelSource tradeCancelWithParties(Long tradeCancelId) {
        TradeCancel cancel = tradeCancelRepository.findByIdWithTradeCompleteAndMember(tradeCancelId)
                .orElseThrow(NotFoundTradeCancelException::new);
        return new TradeCancelSource(
                cancel.getMember().getId(),
                cancel.getTradeComplete().getBuyer().getId(),
                cancel.getTradeComplete().getSeller().getId()
        );
    }

    @Override
    public String noticeTitle(Long noticeId) {
        return noticeRepository.findById(noticeId).orElseThrow(NoticeNotFoundException::new).getTitle();
    }
}
