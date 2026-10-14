package team.startup.gwangsan.application.admin.port.out;

/**
 * 거래 철회 승인·기각. 없으면 거래/상품 업무의 기존 예외를 던진다.
 * 승인은 거래 업무가 상품 ID 스칼라 조회 → 상품 잠금 → 철회 최초 로드 순서로 실행하고, 그 뒤 참여자 지점 검사를 콜백으로 받는다.
 * 기각 경로는 호출자가 productIdOf → lockProduct 순서로 잠근 뒤 rejectPending 을 호출한다.
 */
public interface AdminTradeCancelPort {

    Long productIdOf(Long tradeCancelId);

    void lockProduct(Long productId);

    void approve(Long tradeCancelId, ParticipantCheck check);

    /** PENDING 철회만 REJECTED 로 바꾸고 요청 회원 ID 를 돌려준다. */
    Long rejectPending(Long tradeCancelId);

    Long requesterIdOf(Long tradeCancelId);

    /** 잠금·철회 로드 뒤 구매자·판매자에 대해 실행할 관리자 인가. */
    @FunctionalInterface
    interface ParticipantCheck {
        void check(long buyerId, long sellerId);
    }
}
