package team.startup.gwangsan.application.chat.port.in;

import team.startup.gwangsan.application.chat.data.SaveChatMessageCommand;
import team.startup.gwangsan.application.chat.data.SavedChatMessage;

/**
 * stream 으로 받은 메시지를 멱등 저장한다. 같은 messageId·같은 내용은 저장 결과를 다시 돌려주고(replay),
 * 같은 ID·다른 내용은 ChatMessageIdConflictException 으로 거절한다.
 */
public interface SaveChatMessageUseCase {
    SavedChatMessage execute(SaveChatMessageCommand command);
}
