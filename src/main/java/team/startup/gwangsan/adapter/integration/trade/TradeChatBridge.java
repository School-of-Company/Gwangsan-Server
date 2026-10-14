package team.startup.gwangsan.adapter.integration.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.trade.port.out.TradeChatPort;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;

import java.util.Optional;

/** legacy bridge: chat 공개 query(PR08) 전까지 기존 Repository 로 연결한다. */
@Component
@RequiredArgsConstructor
public class TradeChatBridge implements TradeChatPort {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberJpaRepository memberRepository;

    @Override
    public Optional<Long> findRoomId(long productId, long buyerId, long sellerId) {
        return chatRoomRepository.findByProductIdAndBuyerAndSeller(productId,
                        memberRepository.getReferenceById(buyerId), memberRepository.getReferenceById(sellerId))
                .map(ChatRoom::getId);
    }

    @Override
    public long requireRoomId(long productId, long buyerId, long sellerId) {
        return findRoomId(productId, buyerId, sellerId).orElseThrow(NotFoundChatRoomException::new);
    }

    @Override
    public boolean hasMessageFrom(long roomId, long memberId) {
        return chatMessageRepository.existsByRoomAndSenderId(chatRoomRepository.getReferenceById(roomId), memberId);
    }
}
