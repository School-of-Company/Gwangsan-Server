package team.startup.gwangsan.application.post.data;

/**
 * 게시글 업무가 필요한 채팅방 사실. chat 업무의 Entity 대신 쓴다.
 */
public record ChatRoomView(Long roomId, Long productId, Long buyerId, Long sellerId) {

    /** 기존 ChatRoom.getOtherMember 와 같다: 구매자가 아니면 구매자를 돌려준다. */
    public Long otherOf(Long memberId) {
        return buyerId.equals(memberId) ? sellerId : buyerId;
    }
}
