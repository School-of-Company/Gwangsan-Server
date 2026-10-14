package team.startup.gwangsan.application.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.chat.data.ChatRoomListRow;
import team.startup.gwangsan.application.chat.data.ChatRoomProduct;
import team.startup.gwangsan.application.chat.data.ChatRoomSummary;
import team.startup.gwangsan.application.chat.port.in.ChatRoomListQuery;
import team.startup.gwangsan.application.chat.port.out.ChatProductPort;
import team.startup.gwangsan.application.chat.port.out.ChatRoomListPort;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomListService implements ChatRoomListQuery {

    private final ChatRoomListPort roomListPort;
    private final ChatProductPort productPort;

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomSummary> query(ActorContext actor) {
        List<ChatRoomListRow> rooms = roomListPort.findVisibleRooms(actor.memberId());
        Set<Long> productIds = rooms.stream().map(ChatRoomListRow::productId).collect(Collectors.toSet());
        Map<Long, ChatRoomProduct> products = productPort.findRoomProducts(productIds);
        return rooms.stream()
                .map(room -> new ChatRoomSummary(room,
                        products.getOrDefault(room.productId(), ChatRoomProduct.deleted(room.productId()))))
                .toList();
    }
}
