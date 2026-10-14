package team.startup.gwangsan.application.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.chat.port.in.HideChatRoomUseCase;
import team.startup.gwangsan.application.chat.port.out.ChatRoomPort;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.model.ChatRoom;
import team.startup.gwangsan.domain.common.ActorContext;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HideChatRoomService implements HideChatRoomUseCase {

    private final ChatRoomPort roomPort;

    @Override
    @Transactional
    public void execute(ActorContext actor, Long roomId) {
        ChatRoom room = roomPort.load(roomId).orElseThrow(NotFoundChatRoomException::new);
        // 참여자가 아니면 방 존재 자체를 숨긴다.
        if (!room.isParticipant(actor.memberId())) {
            throw new NotFoundChatRoomException();
        }
        roomPort.saveHiddenState(room.hideFor(actor.memberId(), LocalDateTime.now()));
    }
}
