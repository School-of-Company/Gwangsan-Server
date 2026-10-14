package team.startup.gwangsan.application.chat.port.in;

import team.startup.gwangsan.application.chat.data.ChatRoomMessages;
import team.startup.gwangsan.application.chat.data.LoadChatRoomMessagesCommand;

/** 메시지 페이지 조회. 조회한 최신 메시지까지 상대 메시지를 읽음 처리하므로 쓰기 유스케이스다. */
public interface LoadChatRoomMessagesUseCase {
    ChatRoomMessages execute(LoadChatRoomMessagesCommand command);
}
