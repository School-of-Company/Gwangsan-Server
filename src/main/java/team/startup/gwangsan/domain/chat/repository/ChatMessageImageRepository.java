package team.startup.gwangsan.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.chat.entity.ChatMessageImage;

import java.util.List;

public interface ChatMessageImageRepository extends JpaRepository<ChatMessageImage, Long> {
    List<ChatMessageImage> findAllByChatMessageIdIn(List<Long> chatMessageId);
}
