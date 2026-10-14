package team.startup.gwangsan.application.chat.port.in;

import team.startup.gwangsan.domain.common.ActorContext;

public interface DeleteChatMessageUseCase {
    void execute(ActorContext actor, Long messageId);
}
