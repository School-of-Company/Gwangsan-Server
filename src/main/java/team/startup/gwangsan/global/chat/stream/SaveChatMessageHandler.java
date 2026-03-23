package team.startup.gwangsan.global.chat.stream;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.chat.service.SaveChatMessageService;

@Component
@RequiredArgsConstructor
public class SaveChatMessageHandler implements ChatStreamHandler {

    private final SaveChatMessageService saveChatMessageService;

    @Override
    public void handle(ChatStreamMessage message) {
        saveChatMessageService.execute(
            message.messageId(), message.roomId(), message.content(),
            message.imageIds(), message.messageType(), message.senderId(), message.createdAt()
        );
    }
}
