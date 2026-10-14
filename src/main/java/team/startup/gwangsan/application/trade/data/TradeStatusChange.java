package team.startup.gwangsan.application.trade.data;

import java.time.LocalDateTime;

/** 커밋 후 채팅 서버에 전달하는 거래 상태. 모든 참여자에게 같은 사실만 담는다. */
public record TradeStatusChange(long roomId, long productId, boolean completed, boolean reserved,
                                Boolean requestedBySeller, LocalDateTime requestedAt) {
}
