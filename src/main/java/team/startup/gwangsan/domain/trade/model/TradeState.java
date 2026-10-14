package team.startup.gwangsan.domain.trade.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 한 채팅방의 거래 상태. 채팅방 조회 응답과 거래 상태 변경 전달이 같은 값을 쓰도록 계산을 여기 한 곳에 둔다.
 * 모든 참여자에게 같은 사실만 담는다. 보는 사람마다 다른 값은 {@link #isCompletableFor(boolean)} 로 파생한다.
 */
public record TradeState(boolean completed, boolean reserved, Boolean requestedBySeller, LocalDateTime requestedAt) {

    public static TradeState of(ProductPhase phase, List<TradeRequestState> requests) {
        Optional<TradeRequestState> pending = find(requests, TradeStatus.PENDING);
        return new TradeState(
                phase == ProductPhase.COMPLETED,
                phase == ProductPhase.RESERVED,
                // 대기 중인 요청이 있을 때만 값을 갖는다. 롤백된 요청의 값이 새어 나가면
                // 클라이언트의 isCompletable 계산이 조회 응답과 어긋난다.
                pending.map(TradeRequestState::requestedBySeller).orElse(null),
                // 대기 요청이 없으면 완료된 요청 시각을 유지해 거래 카드와 리뷰 진입점을 남긴다.
                // 철회가 승인되면 둘 다 없으므로 null 이 되어 재요청 버튼이 다시 열린다.
                pending.or(() -> find(requests, TradeStatus.COMPLETED))
                        .map(TradeRequestState::createdAt)
                        .orElse(null)
        );
    }

    /** 대기 요청을 만든 쪽은 스스로 확정할 수 없다. 대기 요청이 없으면 누구든 새로 요청할 수 있다. */
    public boolean isCompletableFor(boolean isSeller) {
        return !completed && (requestedBySeller == null || requestedBySeller != isSeller);
    }

    private static Optional<TradeRequestState> find(List<TradeRequestState> requests, TradeStatus status) {
        return requests.stream().filter(r -> r.status() == status).findFirst();
    }
}
