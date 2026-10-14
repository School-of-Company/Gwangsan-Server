package team.startup.gwangsan.application.alert.port.out;

import java.util.List;

/**
 * 임시 Port: CreateAlertFromSourceUseCase 가 다른 업무의 원천 데이터를 읽는다.
 * 각 조회는 대상이 없으면 해당 업무의 기존 NotFound 예외를 던진다.
 */
public interface LoadAlertSourcePort {

    void requireMember(Long memberId);

    List<Long> existingMemberIds(List<Long> memberIds);

    int recommenderReward();

    TradeCompleteSource tradeComplete(Long tradeCompleteId);

    String activeProductTitle(Long productId);

    ReviewSource review(Long reviewId);

    ReportSource report(Long reportId);

    int suspendedDays(Long suspendId);

    Long tradeCancelRequester(Long tradeCancelId);

    TradeCancelSource tradeCancelWithParties(Long tradeCancelId);

    String noticeTitle(Long noticeId);

    record TradeCompleteSource(Long productId, String productTitle, Long buyerId, Long sellerId, String buyerNickname) {
    }

    record ReviewSource(Long reviewerId, String reviewerNickname) {
    }

    record ReportSource(Long reporterId, String reportedNickname) {
    }

    record TradeCancelSource(Long requesterId, Long buyerId, Long sellerId) {
    }
}
