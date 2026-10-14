package team.startup.gwangsan.application.chat.port.in;

import team.startup.gwangsan.application.chat.data.UpdatedChatMessage;
import team.startup.gwangsan.domain.common.ActorContext;

public interface UpdateChatMessageUseCase {
    UpdatedChatMessage execute(ActorContext actor, Long messageId, String content);
}
