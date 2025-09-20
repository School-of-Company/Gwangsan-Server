package team.startup.gwangsan.domain.chat.repository.custom;

import team.startup.gwangsan.domain.chat.entity.ChatMessageImage;

import java.util.List;

public interface ChatMessageImageCustomRepository {
    List<ChatMessageImage> findAllByChatMessageIdIn(List<Long> chatMessageId);
}
