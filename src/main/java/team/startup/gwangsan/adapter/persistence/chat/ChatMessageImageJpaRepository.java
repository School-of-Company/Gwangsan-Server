package team.startup.gwangsan.adapter.persistence.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ChatMessageImageJpaRepository extends JpaRepository<ChatMessageImageJpaEntity, Long>, ChatMessageImageCustomRepository {

    @Modifying
    @Query("DELETE FROM ChatMessageImageJpaEntity image WHERE image.chatMessage.id = :messageId")
    void deleteByMessageId(Long messageId);
}
