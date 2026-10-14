package team.startup.gwangsan.application.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.chat.port.in.ChatRoomIdQuery;
import team.startup.gwangsan.application.chat.port.out.ChatRoomPort;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.model.ChatRoom;
import team.startup.gwangsan.domain.common.ActorContext;

@Service
@RequiredArgsConstructor
public class ChatRoomIdService implements ChatRoomIdQuery {

    private final ChatRoomPort roomPort;

    @Override
    @Transactional(readOnly = true)
    public Long query(ActorContext actor, Long productId) {
        return roomPort.findByProductAndMember(productId, actor.memberId())
                .map(ChatRoom::id)
                .orElseThrow(NotFoundChatRoomException::new);
    }
}
