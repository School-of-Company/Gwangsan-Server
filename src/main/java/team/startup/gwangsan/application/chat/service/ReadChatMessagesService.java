package team.startup.gwangsan.application.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.chat.port.in.ReadChatMessagesUseCase;
import team.startup.gwangsan.application.chat.port.out.ChatMessageQueryPort;
import team.startup.gwangsan.domain.common.ActorContext;

@Service
@RequiredArgsConstructor
public class ReadChatMessagesService implements ReadChatMessagesUseCase {

    private final ChatMessageQueryPort messageQueryPort;

    @Override
    @Transactional
    public void execute(ActorContext actor, Long roomId, Long lastMessageId) {
        messageQueryPort.markRead(roomId, lastMessageId, actor.memberId());
    }
}
