package team.startup.gwangsan.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.repository.custom.ChatMessageCustomRepository;
import team.startup.gwangsan.domain.member.entity.Member;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageCustomRepository {
    boolean existsByRoomAndSenderId(ChatRoom room, Long senderId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatMessage c SET c.sender = :dummy WHERE c.sender = :target")
    void reassignSender(@Param("target") Member target, @Param("dummy") Member dummy);
}
