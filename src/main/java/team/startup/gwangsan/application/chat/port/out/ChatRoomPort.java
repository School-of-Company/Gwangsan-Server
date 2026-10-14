package team.startup.gwangsan.application.chat.port.out;

import team.startup.gwangsan.domain.chat.model.ChatRoom;

import java.util.Optional;

public interface ChatRoomPort {

    Optional<ChatRoom> load(Long roomId);

    /** 메시지 화면용. 같은 트랜잭션의 상품 카드 조회가 상품/판매자를 다시 읽지 않도록 함께 적재한다. */
    Optional<ChatRoom> loadWithProduct(Long roomId);

    Optional<ChatRoom> findByProductAndParticipants(Long productId, Long buyerId, Long sellerId);

    Optional<ChatRoom> findByProductAndMember(Long productId, Long memberId);

    Long create(Long productId, Long buyerId, Long sellerId);

    /** 참여자별 숨김 시각을 명시적으로 기록한다. */
    void saveHiddenState(ChatRoom room);
}
