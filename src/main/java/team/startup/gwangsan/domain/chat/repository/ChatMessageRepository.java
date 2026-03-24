package team.startup.gwangsan.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.repository.custom.ChatMessageCustomRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageCustomRepository {
    boolean existsByRoomAndSenderId(ChatRoom room, Long senderId);
}
