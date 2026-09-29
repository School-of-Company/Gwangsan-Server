package team.startup.gwangsan.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import team.startup.gwangsan.domain.chat.entity.ChatMessageImage;
import team.startup.gwangsan.domain.chat.repository.custom.ChatMessageImageCustomRepository;

public interface ChatMessageImageRepository extends JpaRepository<ChatMessageImage, Long>, ChatMessageImageCustomRepository {
    @Modifying
    @Query("DELETE FROM ChatMessageImage image WHERE image.chatMessage.id = :messageId")
    void deleteByMessageId(Long messageId);
}
