package team.startup.gwangsan.domain.alert.model;

import java.time.LocalDateTime;

/**
 * 회원 알림함 항목. 저장 전에는 id·createdAt 이 null 이다.
 * 정적 팩토리가 유형별 제목/본문 문구를 소유한다 (기존 CreateAlertServiceImpl 문구와 동일).
 */
public record Alert(
        Long id,
        Long sourceId,
        Long senderMemberId,
        AlertType type,
        String title,
        String content,
        LocalDateTime createdAt
) {
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
    private static final String NOTICE_CONTENT = "공지가 등록되었습니다.";
    private static final String FEEDBACK_ANSWER_TITLE = "문의 답변 등록";
    private static final String FEEDBACK_ANSWER_CONTENT = "문의하신 내용에 답변이 등록되었습니다.";

    private static Alert of(Long sourceId, Long senderMemberId, AlertType type, String title, String content) {
        return new Alert(null, sourceId, senderMemberId, type, title, content, null);
    }

    /** 구매자에게 가는 거래 완료 알림. 발신자는 판매자다. */
    public static Alert tradeCompleteForBuyer(Long productId, Long sellerId, String productTitle) {
        return of(productId, sellerId, AlertType.TRADE_COMPLETE, productTitle, BUYER_TRADE_BODY);
    }

    /** 판매자에게 가는 거래 완료 알림. 발신자는 구매자다. */
    public static Alert tradeCompleteForSeller(Long productId, Long buyerId, String productTitle) {
        return of(productId, buyerId, AlertType.TRADE_COMPLETE, productTitle, SELLER_TRADE_BODY);
    }

    public static Alert tradeCompleteReject(Long productId, String productTitle) {
        return of(productId, null, AlertType.TRADE_COMPLETE_REJECT, productTitle, TRADE_COMPLETE_REJECT_CONTENT);
    }

    // 기존 동작: 발신자는 항상 판매자, 본문 이름은 항상 구매자다.
    public static Alert otherMemberTradeComplete(Long productId, Long sellerId, String productTitle, String buyerNickname) {
        return of(productId, sellerId, AlertType.OTHER_MEMBER_TRADE_COMPLETE, productTitle,
                buyerNickname + OTHER_MEMBER_TRADE_COMPLETE_CONTENT);
    }

    public static Alert recommender(Long signUpMemberId, int reward) {
        return of(signUpMemberId, signUpMemberId, AlertType.RECOMMENDER, RECOMMENDER_TITLE,
                String.format(RECOMMENDER_CONTENT, reward));
    }

    public static Alert review(Long reviewId, Long reviewerId, String reviewerNickname) {
        return of(reviewId, reviewerId, AlertType.REVIEW, reviewerNickname + REVIEW_TITLE, REVIEW_CONTENT);
    }

    public static Alert reportReject(Long reportId, Long reporterId, String reportedNickname) {
        return of(reportId, reporterId, AlertType.REPORT_REJECT, REPORT_REJECT_TITLE,
                reportedNickname + REPORT_REJECT_CONTENT);
    }

    public static Alert tradeCancelReject(Long tradeCancelId, Long requesterId) {
        return of(tradeCancelId, requesterId, AlertType.TRADE_CANCEL_REJECT, TRADE_CANCEL_REJECT_TITLE,
                TRADE_CANCEL_REJECT_CONTENT);
    }

    public static Alert tradeCancel(Long tradeCancelId, Long requesterId) {
        return of(tradeCancelId, requesterId, AlertType.TRADE_CANCEL, TRADE_CANCEL_TITLE, TRADE_CANCEL_CONTENT);
    }

    public static Alert report(Long sourceId, Long reporterId, int suspendedDays) {
        return of(sourceId, reporterId, AlertType.REPORT, REPORT_TITLE, suspendedDays + REPORT_CONTENT);
    }

    public static Alert notice(Long noticeId, String noticeTitle) {
        return of(noticeId, null, AlertType.NOTICE, noticeTitle, NOTICE_CONTENT);
    }

    public static Alert feedbackAnswer(Long feedbackId) {
        return of(feedbackId, null, AlertType.FEEDBACK_ANSWER, FEEDBACK_ANSWER_TITLE, FEEDBACK_ANSWER_CONTENT);
    }
}
