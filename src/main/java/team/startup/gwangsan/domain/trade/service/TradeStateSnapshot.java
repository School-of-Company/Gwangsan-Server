package team.startup.gwangsan.domain.trade.service;

import java.time.LocalDateTime;

/**
 * 한 채팅방(상품 + 구매자 + 판매자)의 거래 상태 스냅샷.
 *
 * <p>채팅방 조회 응답과 거래 상태 변경 이벤트가 <b>같은 값</b>을 쓰도록 하기 위한 타입이다.
 * 두 경로가 각자 계산하면 같은 상태를 다르게 표현해 클라이언트 화면이 어긋난다.
 *
 * <p>여기에는 모든 참여자에게 동일한 사실만 담는다. 보는 사람에 따라 달라지는 값은
 * {@link #isCompletableFor(boolean)} 처럼 조회 시점에 파생시키고, 방 전체로 발행하지 않는다.
 */
public record TradeStateSnapshot(
        boolean completed,
        boolean reserved,
        Boolean requestedBySeller,
        LocalDateTime requestedAt
) {

    /**
     * 이 사람이 거래 완료를 확정할 수 있는지.
     *
     * <p>대기 중인 요청을 만든 쪽은 스스로 확정할 수 없으므로 역할이 같으면 false 다.
     * 대기 중인 요청이 아예 없으면 누구든 새로 요청할 수 있으므로 true 다.
     *
     * <p><b>이 값은 viewer 마다 다르므로 이벤트 페이로드에 넣지 않는다.</b> 클라이언트는
     * {@link #requestedBySeller()} 와 자신의 isSeller 를 조합해 같은 결과를 얻는다.
     */
    public boolean isCompletableFor(boolean isSeller) {
        return !completed && (requestedBySeller == null || requestedBySeller != isSeller);
    }
}
