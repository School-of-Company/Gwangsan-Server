package team.startup.gwangsan.application.chat.data;

/** 다른 업무에 공개하는 채팅방 사실. */
public record ChatRoomSnapshot(Long roomId, Long productId, Long buyerId, Long sellerId) {
}
