package team.startup.gwangsan.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    boolean existsByProductIdAndSenderId(Long productId, Long senderId);
}
