package team.startup.gwangsan.adapter.integration.alert;

import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.application.notice.port.in.NoticeQuery;
import team.startup.gwangsan.application.report.data.ReportAlertSource;
import team.startup.gwangsan.application.report.port.in.ReportQuery;
import team.startup.gwangsan.application.suspend.port.in.SuspensionQuery;
import team.startup.gwangsan.application.trade.port.in.TradeCancelQuery;
import team.startup.gwangsan.adapter.persistence.review.ReviewJpaEntity;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;
import team.startup.gwangsan.adapter.persistence.review.ReviewJpaRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaEntity;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompleteJpaEntity;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompleteJpaRepository;

import java.util.List;

import static team.startup.gwangsan.domain.admin.policy.SignUpReward.RECOMMENDER_GWANGSAN_REWARD;

/**
 * 임시 bridge: 기존 CreateAlertServiceImpl 이 하던 원천 조회를 수행한다. 비동기 알림 경로는 유지되므로 Port 는 남고, 구현만 각 업무의 공개 조회로 교체한다.
 * 회원·상품·신고·정지·공지·철회 요청자는 공개 Port 로 교체됨. 남은 trade/review 저장 모델 직접 사용
 * (tradeComplete·tradeCancelWithParties·review) 은 PR07 이 알림용 공개 조회를 제공하면 교체하고 이름에서 Legacy 를 뺀다.
 */
@Component
@RequiredArgsConstructor
public class LegacyAlertSourceAdapter implements LoadAlertSourcePort {

    private final MemberAccountQuery memberAccountQuery;
    private final TradeCompleteJpaRepository tradeCompleteRepository;
    private final ProductSnapshotQuery productSnapshotQuery;
    private final ReviewJpaRepository reviewRepository;
    private final ReportQuery reportQuery;
    private final SuspensionQuery suspensionQuery;
    private final TradeCancelJpaRepository tradeCancelRepository;
    private final TradeCancelQuery tradeCancelQuery;
    private final NoticeQuery noticeQuery;

    @Override
    public void requireMember(Long memberId) {
        memberAccountQuery.findById(memberId).orElseThrow(NotFoundMemberException::new);
    }

    @Override
    public List<Long> existingMemberIds(List<Long> memberIds) {
        return memberAccountQuery.findExistingIds(memberIds);
    }

    @Override
    public int recommenderReward() {
        return RECOMMENDER_GWANGSAN_REWARD;
    }

    @Override
    public TradeCompleteSource tradeComplete(Long tradeCompleteId) {
        TradeCompleteJpaEntity trade = tradeCompleteRepository.findById(tradeCompleteId)
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
        // 삭제된 게시글이면 NotFoundProductException (기존과 같음)
        return productSnapshotQuery.getActive(productId).title();
    }

    @Override
    public ReviewSource review(Long reviewId) {
        ReviewJpaEntity review = reviewRepository.findById(reviewId).orElseThrow(NotFoundReviewException::new);
        return new ReviewSource(review.getReviewer().getId(), review.getReviewer().getNickname());
    }

    @Override
    public ReportSource report(Long reportId) {
        ReportAlertSource source = reportQuery.getAlertSource(reportId);
        return new ReportSource(source.reporterId(), source.reportedNickname());
    }

    @Override
    public int suspendedDays(Long suspendId) {
        return suspensionQuery.getSuspendedDays(suspendId);
    }

    @Override
    public Long tradeCancelRequester(Long tradeCancelId) {
        return tradeCancelQuery.findRequesterId(tradeCancelId).orElseThrow(NotFoundTradeCancelException::new);
    }

    @Override
    public TradeCancelSource tradeCancelWithParties(Long tradeCancelId) {
        TradeCancelJpaEntity cancel = tradeCancelRepository.findByIdWithTradeCompleteAndMember(tradeCancelId)
                .orElseThrow(NotFoundTradeCancelException::new);
        return new TradeCancelSource(
                cancel.getMember().getId(),
                cancel.getTradeComplete().getBuyer().getId(),
                cancel.getTradeComplete().getSeller().getId()
        );
    }

    @Override
    public String noticeTitle(Long noticeId) {
        return noticeQuery.getTitle(noticeId);
    }
}
