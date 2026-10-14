package team.startup.gwangsan.application.chat.data;

/** 채팅방 생성에 필요한 활성 상품 사실. giver 이면 상품 작성자가 판매자다. */
public record ChatProduct(Long productId, Long ownerId, boolean giver) {
}
