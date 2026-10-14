package team.startup.gwangsan.adapter.persistence.chat;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.chat.data.ChatRoomListRow;
import team.startup.gwangsan.application.chat.port.out.ChatRoomListPort;
import team.startup.gwangsan.application.chat.port.out.ChatRoomPort;
import team.startup.gwangsan.domain.chat.model.ChatRoom;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatRoomPersistenceAdapter implements ChatRoomPort, ChatRoomListPort {

    private final ChatRoomJpaRepository rooms;
    private final EntityManager entityManager;

    @Override
    public Optional<ChatRoom> load(Long roomId) {
        return rooms.findChatRoomByRoomId(roomId).map(ChatRoomPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<ChatRoom> loadWithProduct(Long roomId) {
        return rooms.findByRoomIdWithSellerAndProduct(roomId).map(ChatRoomPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<ChatRoom> findByProductAndParticipants(Long productId, Long buyerId, Long sellerId) {
        return rooms.findByProductIdAndBuyerAndSeller(productId, member(buyerId), member(sellerId))
                .map(ChatRoomPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<ChatRoom> findByProductAndMember(Long productId, Long memberId) {
        return rooms.findByProductIdAndMember(productId, member(memberId)).map(ChatRoomPersistenceAdapter::toDomain);
    }

    @Override
    public Long create(Long productId, Long buyerId, Long sellerId) {
        ChatRoomJpaEntity room = ChatRoomJpaEntity.builder()
                .buyer(member(buyerId))
                .seller(member(sellerId))
                .isActive(true)
                .product(entityManager.getReference(Product.class, productId))
                .build();
        return rooms.save(room).getId();
    }

    @Override
    public void saveHiddenState(ChatRoom room) {
        rooms.getReferenceById(room.id()).updateHiddenState(room.hiddenByBuyerAt(), room.hiddenBySellerAt());
    }

    @Override
    public List<ChatRoomListRow> findVisibleRooms(Long memberId) {
        return rooms.findRoomsByMemberId(memberId);
    }

    // 조회 조건과 FK 값에만 쓰는 참조. 이미 같은 트랜잭션에 적재된 회원이면 그 인스턴스를 돌려준다.
    private MemberJpaEntity member(Long memberId) {
        return entityManager.getReference(MemberJpaEntity.class, memberId);
    }

    static ChatRoom toDomain(ChatRoomJpaEntity room) {
        return new ChatRoom(
                room.getId(),
                room.getProduct() == null ? null : room.getProduct().getId(),
                room.getBuyer().getId(),
                room.getSeller().getId(),
                room.getHiddenByBuyerAt(),
                room.getHiddenBySellerAt());
    }
}
