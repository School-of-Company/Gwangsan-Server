package team.startup.gwangsan.application.trade.port.in;

/**
 * 관리자 철회 기각. 호출자가 먼저 같은 상품 행을 잠근 뒤(삭제 포함 잠금) 호출해야 한다.
 * PENDING 이 아니면 CannotPendingTradeCancelException, 없으면 NotFoundTradeCancelException.
 */
public interface RejectTradeCancelUseCase {
    /** @return 철회 요청 회원 ID */
    long reject(long tradeCancelId);
}
