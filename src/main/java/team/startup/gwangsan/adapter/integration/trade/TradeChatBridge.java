package team.startup.gwangsan.adapter.integration.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.chat.data.ChatRoomSnapshot;
import team.startup.gwangsan.application.chat.port.in.ChatRoomLookupQuery;
import team.startup.gwangsan.application.trade.port.out.TradeChatPort;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;

import java.util.Optional;

/** trade → chat 공개 query. */
@Component
@RequiredArgsConstructor
public class TradeChatBridge implements TradeChatPort {

    private final ChatRoomLookupQuery chatRoomLookupQuery;

    @Override
    public Optional<Long> findRoomId(long productId, long buyerId, long sellerId) {
        return chatRoomLookupQuery.byProductAndParticipants(productId, buyerId, sellerId).map(ChatRoomSnapshot::roomId);
    }

    @Override
    public long requireRoomId(long productId, long buyerId, long sellerId) {
        return findRoomId(productId, buyerId, sellerId).orElseThrow(NotFoundChatRoomException::new);
    }

    @Override
    public boolean hasMessageFrom(long roomId, long memberId) {
        return chatRoomLookupQuery.hasSentMessage(roomId, memberId);
    }
}
