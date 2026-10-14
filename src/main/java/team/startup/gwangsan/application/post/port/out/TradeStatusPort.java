package team.startup.gwangsan.application.post.port.out;

public interface TradeStatusPort {
    /** 현재 거래 상태를 채팅방 참여자에게 커밋 후 전달한다. 상품 상태를 저장한 뒤 호출한다. */
    void publishChanged(Long roomId, Long productId, Long buyerId, Long sellerId);
}
