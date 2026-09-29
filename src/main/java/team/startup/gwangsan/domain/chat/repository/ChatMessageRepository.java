package team.startup.gwangsan.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.repository.custom.ChatMessageCustomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageCustomRepository {
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ChatMessage c WHERE c.room = :room AND c.sender.id = :senderId AND c.deletedAt IS NULL")
    boolean existsByRoomAndSenderId(@Param("room") ChatRoom room, @Param("senderId") Long senderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChatMessage c WHERE c.id = :id")
    Optional<ChatMessage> findByIdForUpdate(@Param("id") Long id);

    Optional<ChatMessage> findFirstByRoomIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long roomId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatMessage c SET c.sender = :dummy WHERE c.sender = :target")
    void reassignSender(@Param("target") Member target, @Param("dummy") Member dummy);
}
