package team.startup.gwangsan.application.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.chat.data.ChatRoomSnapshot;
import team.startup.gwangsan.application.chat.port.in.ChatRoomLookupQuery;
import team.startup.gwangsan.application.chat.port.out.ChatMessagePort;
import team.startup.gwangsan.application.chat.port.out.ChatRoomPort;
import team.startup.gwangsan.domain.chat.model.ChatRoom;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomLookupService implements ChatRoomLookupQuery {

    private final ChatRoomPort roomPort;
    private final ChatMessagePort messagePort;

    // 호출자 트랜잭션이 있으면 참여한다 (거래 완료 등 잠금 이후 조회).
    @Override
    @Transactional(readOnly = true)
    public Optional<ChatRoomSnapshot> byRoomId(Long roomId) {
        return roomPort.load(roomId).map(ChatRoomLookupService::snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatRoomSnapshot> byProductAndParticipants(Long productId, Long buyerId, Long sellerId) {
        return roomPort.findByProductAndParticipants(productId, buyerId, sellerId).map(ChatRoomLookupService::snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatRoomSnapshot> byProductAndMember(Long productId, Long memberId) {
        return roomPort.findByProductAndMember(productId, memberId).map(ChatRoomLookupService::snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSentMessage(Long roomId, Long memberId) {
        return messagePort.hasSentMessage(roomId, memberId);
    }

    private static ChatRoomSnapshot snapshot(ChatRoom room) {
        return new ChatRoomSnapshot(room.id(), room.productId(), room.buyerId(), room.sellerId());
    }
}
