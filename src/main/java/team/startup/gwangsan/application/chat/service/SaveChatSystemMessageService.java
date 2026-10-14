package team.startup.gwangsan.application.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.chat.port.in.SaveChatSystemMessageUseCase;
import team.startup.gwangsan.application.chat.port.out.ChatMessagePort;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SaveChatSystemMessageService implements SaveChatSystemMessageUseCase {

    private final ChatMessagePort messagePort;

    @Override
    @Transactional
    public Long save(Long roomId, Long messageId, Long senderId, String content, LocalDateTime createdAt) {
        return messagePort.saveSystemMessage(roomId, messageId, senderId, content, createdAt);
    }
}
