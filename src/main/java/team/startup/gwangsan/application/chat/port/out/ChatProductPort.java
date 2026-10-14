package team.startup.gwangsan.application.chat.port.out;

import team.startup.gwangsan.application.chat.data.ChatProduct;
import team.startup.gwangsan.application.chat.data.ChatRoomProduct;
import team.startup.gwangsan.application.chat.data.ChatTradeCard;

import java.util.Map;
import java.util.Set;

/** 상품·거래 상태 조회 (post/trade 소유). */
public interface ChatProductPort {

    /** 활성 상품. 없으면 상품 부재 오류(404). */
    ChatProduct loadActive(Long productId);

    /** 조회되지 않는 상품은 결과에서 빠진다. */
    Map<Long, ChatRoomProduct> findRoomProducts(Set<Long> productIds);

    ChatTradeCard loadTradeCard(Long productId, Long buyerId, Long sellerId);
}
