package team.startup.gwangsan.application.trade.data;

import team.startup.gwangsan.domain.trade.model.TradeState;

import java.time.LocalDateTime;

/**
 * 한 채팅방의 거래 상태 공개 계약. 채팅방 조회 응답과 거래 상태 변경 전달이 같은 값을 쓴다.
 * 보는 사람마다 다른 값은 {@link #isCompletableFor(boolean)} 로만 파생하고 방 전체로 보내지 않는다.
 * 계산 규칙은 {@link TradeState} 가 소유한다.
 */
public record TradeStateSnapshot(boolean completed, boolean reserved, Boolean requestedBySeller, LocalDateTime requestedAt) {

    public boolean isCompletableFor(boolean isSeller) {
        return new TradeState(completed, reserved, requestedBySeller, requestedAt).isCompletableFor(isSeller);
    }
}
