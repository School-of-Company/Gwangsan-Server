package team.startup.gwangsan.application.trade.port.in;

/**
 * 회원 탈퇴 시 거래 업무의 회원 참조를 dummy 회원으로 재지정한다(member 탈퇴 cleanup 소비).
 * 각 메서드는 즉시 실행되는 bulk UPDATE 이고 실행 뒤 영속성 컨텍스트를 비운다(clearAutomatically).
 * 기존 순서: 거래 구매자 → 판매자 재지정(다른 업무 정리) … 철회 요청자 재지정.
 */
public interface TradeMemberReferenceCleanupUseCase {

    /** tbl_trade_complete.buyer_id 다음 seller_id */
    void reassignTradeParties(Long memberId, Long dummyMemberId);

    /** tbl_trade_cancel.member_id */
    void reassignCancelRequester(Long memberId, Long dummyMemberId);
}
