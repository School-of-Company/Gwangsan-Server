package team.startup.gwangsan.adapter.persistence.chat;

import java.util.List;

public interface ChatMessageImageCustomRepository {
    List<ChatMessageImageJpaEntity> findAllByChatMessageIdIn(List<Long> chatMessageId);
}
