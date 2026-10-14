package team.startup.gwangsan.application.chat.port.in;

import team.startup.gwangsan.application.chat.data.ChatRoomSummary;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;

public interface ChatRoomListQuery {
    List<ChatRoomSummary> query(ActorContext actor);
}
