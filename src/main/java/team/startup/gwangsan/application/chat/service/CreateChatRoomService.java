package team.startup.gwangsan.application.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.chat.data.ChatProduct;
import team.startup.gwangsan.application.chat.port.in.CreateChatRoomUseCase;
import team.startup.gwangsan.application.chat.port.out.ChatBlockPort;
import team.startup.gwangsan.application.chat.port.out.ChatProductPort;
import team.startup.gwangsan.application.chat.port.out.ChatRoomPort;
import team.startup.gwangsan.domain.chat.model.ChatRoom;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateChatRoomService implements CreateChatRoomUseCase {

    private final ChatProductPort productPort;
    private final ChatBlockPort blockPort;
    private final ChatRoomPort roomPort;

    @Override
    @Transactional
    public Long execute(ActorContext actor, Long productId) {
        Long memberId = actor.memberId();
        ChatProduct product = productPort.loadActive(productId);
        blockPort.validateNotBlocked(memberId, product.ownerId());

        Long buyerId = product.giver() ? memberId : product.ownerId();
        Long sellerId = product.giver() ? product.ownerId() : memberId;

        Optional<ChatRoom> existing = roomPort.findByProductAndParticipants(productId, buyerId, sellerId);
        if (existing.isPresent()) {
            ChatRoom room = existing.get();
            roomPort.saveHiddenState(room.unhideFor(memberId));
            return room.id();
        }
        return roomPort.create(productId, buyerId, sellerId);
    }
}
