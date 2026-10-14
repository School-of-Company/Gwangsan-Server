package team.startup.gwangsan.application.trade.data;

/**
 * 관리자 철회 승인. 관리자 인가는 승인 진입(admin)이 소유하지만, 기존 순서대로
 * 상품 잠금과 철회 요청 로드 뒤 구매자·판매자에 대해 실행돼야 하므로 콜백으로 받는다.
 */
public record ApproveTradeCancelCommand(long tradeCancelId, ParticipantAuthorizer authorizer) {

    @FunctionalInterface
    public interface ParticipantAuthorizer {
        void authorize(long buyerId, long sellerId);
    }
}
