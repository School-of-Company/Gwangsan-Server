package team.startup.gwangsan.adapter.integration.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.notice.port.in.NoticeQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.application.report.data.ReportAlertSource;
import team.startup.gwangsan.application.report.port.in.ReportQuery;
import team.startup.gwangsan.application.review.data.ReviewAuthor;
import team.startup.gwangsan.application.review.port.in.ReviewSummaryQuery;
import team.startup.gwangsan.application.suspend.port.in.SuspensionQuery;
import team.startup.gwangsan.application.trade.data.TradeCancelParties;
import team.startup.gwangsan.application.trade.data.TradeCompleteSummary;
import team.startup.gwangsan.application.trade.port.in.TradeCancelQuery;
import team.startup.gwangsan.application.trade.port.in.TradeSummaryQuery;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;

import java.util.List;

import static team.startup.gwangsan.domain.admin.policy.SignUpReward.RECOMMENDER_GWANGSAN_REWARD;

/**
 * 비동기 알림 경로의 원천 조회를 각 업무 공개 조회로 수행한다. 원천이 없으면 각 업무의 기존 NotFound 예외가 그대로 나간다.
 */
@Component
@RequiredArgsConstructor
public class AlertSourceAdapter implements LoadAlertSourcePort {

    private final MemberAccountQuery memberAccountQuery;
    private final TradeSummaryQuery tradeSummaryQuery;
    private final ProductSnapshotQuery productSnapshotQuery;
    private final ReviewSummaryQuery reviewSummaryQuery;
    private final ReportQuery reportQuery;
    private final SuspensionQuery suspensionQuery;
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
        TradeCompleteSummary trade = tradeSummaryQuery.getTradeComplete(tradeCompleteId);
        return new TradeCompleteSource(
                trade.productId(),
                trade.productTitle(),
                trade.buyerId(),
                trade.sellerId(),
                trade.buyerNickname()
        );
    }

    @Override
    public String activeProductTitle(Long productId) {
        // 삭제된 게시글이면 NotFoundProductException (기존과 같음)
        return productSnapshotQuery.getActive(productId).title();
    }

    @Override
    public ReviewSource review(Long reviewId) {
        ReviewAuthor author = reviewSummaryQuery.getAuthor(reviewId);
        return new ReviewSource(author.reviewerId(), author.reviewerNickname());
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
        TradeCancelParties parties = tradeSummaryQuery.getCancelParties(tradeCancelId);
        return new TradeCancelSource(parties.requesterId(), parties.buyerId(), parties.sellerId());
    }

    @Override
    public String noticeTitle(Long noticeId) {
        return noticeQuery.getTitle(noticeId);
    }
}
