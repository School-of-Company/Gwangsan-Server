package team.startup.gwangsan.global.event;

import java.time.LocalDateTime;

/**
 * 거래 상태가 바뀌어 채팅 서버가 양쪽 참여자 화면을 갱신해야 함을 알린다.
 *
 * <p>여기 담기는 값은 모든 참여자에게 동일한 사실이어야 한다. isCompletable 처럼
 * 보는 사람의 isSeller 에 따라 달라지는 값은 방 전체로 발행할 수 없으므로,
 * 대신 {@code requestedBySeller} 를 보내고 클라이언트가 자신의 isSeller 와 조합해 계산한다.
 */
public record TradeStatusChangedEvent(
        Long roomId,
        Long productId,
        boolean completed,
        boolean reserved,
        /** 완료 요청을 먼저 한 쪽이 판매자인지. 대기 중인 요청이 없으면 null. */
        Boolean requestedBySeller,
        /** 거래 요청 생성 시각. 활성 요청도 완료된 요청도 없으면 null. */
        LocalDateTime requestedAt
) {
}
