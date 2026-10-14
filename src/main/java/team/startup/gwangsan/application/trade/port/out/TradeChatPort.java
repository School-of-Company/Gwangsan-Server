package team.startup.gwangsan.application.trade.port.out;

import java.util.Optional;

/** 거래 채팅방(chat 소유). */
public interface TradeChatPort {
    Optional<Long> findRoomId(long productId, long buyerId, long sellerId);

    /** 없으면 채팅 소유자의 기존 오류. */
    long requireRoomId(long productId, long buyerId, long sellerId);

    boolean hasMessageFrom(long roomId, long memberId);
}
