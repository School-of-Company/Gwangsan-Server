package team.startup.gwangsan.application.trade.port.out;

/**
 * 거래 알림. record* 는 중요한 업무 알림이라 호출자 트랜잭션 안에서 동기 저장한다(실패 시 업무 rollback).
 */
public interface TradeAlertPort {
    /** 거래 완료 요청 알림(OTHER_MEMBER_TRADE_COMPLETE). 수신자는 요청 상대(receiverId). */
    void recordCompletionRequested(long productId, long buyerId, long sellerId, long receiverId);

    /** 철회 완료 알림(TRADE_CANCEL). 수신자는 구매자·판매자 양쪽. */
    void recordCancelApproved(long tradeCancelId, long requesterId, long buyerId, long sellerId);

    /** 관리자 승인 대기 알림 요청(기존 커밋 후 처리 유지). */
    void requestCancelReview(long tradeCancelId, long requesterId);

    /** 처리된 철회 요청의 관리자 알림 제거. */
    void removeCancelReview(long tradeCancelId);
}
