package team.startup.gwangsan.adapter.persistence.chat;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.util.Optional;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageJpaEntity, Long>, ChatMessageCustomRepository {

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ChatMessageJpaEntity c WHERE c.room = :room AND c.sender.id = :senderId AND c.deletedAt IS NULL")
    boolean existsByRoomAndSenderId(@Param("room") ChatRoomJpaEntity room, @Param("senderId") Long senderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChatMessageJpaEntity c WHERE c.id = :id")
    Optional<ChatMessageJpaEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<ChatMessageJpaEntity> findFirstByRoomIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long roomId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatMessageJpaEntity c SET c.sender = :dummy WHERE c.sender = :target")
    void reassignSender(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}
