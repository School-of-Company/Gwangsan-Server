package team.startup.gwangsan.application.chat.port.in;

import team.startup.gwangsan.domain.common.ActorContext;

public interface HideChatRoomUseCase {
    void execute(ActorContext actor, Long roomId);
}
