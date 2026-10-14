package team.startup.gwangsan.adapter.persistence.chat;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.application.chat.data.SaveChatMessageCommand;
import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.chat.model.StoredChatMessage;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static team.startup.gwangsan.adapter.persistence.chat.QChatMessageJpaEntity.chatMessageJpaEntity;
import static team.startup.gwangsan.adapter.persistence.member.QMemberJpaEntity.memberJpaEntity;

@Repository
@RequiredArgsConstructor
public class ChatMessageCustomRepositoryImpl implements ChatMessageCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean insertIfAbsent(SaveChatMessageCommand message) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO tbl_chat_message
                        (message_id, room_id, sender_id, content, message_type, created_at, checked)
                    VALUES (:id, :roomId, :senderId, :content, :messageType, :createdAt, false)
                    """, new MapSqlParameterSource()
                    .addValue("id", message.messageId())
                    .addValue("roomId", message.roomId())
                    .addValue("senderId", message.senderId())
                    .addValue("content", message.content())
                    .addValue("messageType", message.messageType().name())
                    .addValue("createdAt", message.createdAt()));
            return true;
        } catch (DuplicateKeyException e) {
            // Catch the JDBC statement failure here, before a transaction interceptor can mark rollback-only.
            if (isMariaDbDuplicate(e) && findStoredMessage(message.messageId()).isPresent()) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public Optional<StoredChatMessage> findStoredMessage(Long messageId) {
        // A locking read sees the winner's commit even after an earlier REPEATABLE READ snapshot.
        ChatMessageJpaEntity message = em.find(ChatMessageJpaEntity.class, messageId, LockModeType.PESSIMISTIC_READ);
        if (message == null) return Optional.empty();
        List<ChatImage> images = em.createQuery("""
                        SELECT link FROM ChatMessageImageJpaEntity link JOIN FETCH link.image
                        WHERE link.chatMessage.id = :messageId ORDER BY link.id
                        """, ChatMessageImageJpaEntity.class)
                .setParameter("messageId", messageId)
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .getResultList().stream()
                .map(link -> new ChatImage(link.getImage().getId(), link.getImage().getImageUrl()))
                .toList();
        return Optional.of(new StoredChatMessage(messageId, message.getRoom().getId(),
                message.getSender() == null ? null : message.getSender().getId(),
                message.getOriginalContent() == null ? message.getContent() : message.getOriginalContent(),
                message.getMessageType(), message.getCreatedAt(), message.getChecked(),
                message.getDeletedAt() != null, images));
    }

    @Override
    public Set<Long> findExistingImageIds(List<Long> imageIds) {
        if (imageIds.isEmpty()) return Set.of();
        return new HashSet<>(jdbcTemplate.queryForList("""
                SELECT image_id FROM tbl_image WHERE image_id IN (:imageIds) LOCK IN SHARE MODE
                """, Map.of("imageIds", imageIds), Long.class));
    }

    private boolean isMariaDbDuplicate(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && sqlException.getErrorCode() == 1062) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ChatMessageJpaEntity> findChatMessageByRoomIdWithCursorPaging(Long roomId, LocalDateTime lastCreatedAt, Long lastMessageId, int limit) {
        return queryFactory
                .selectFrom(chatMessageJpaEntity)
                .join(chatMessageJpaEntity.sender, memberJpaEntity).fetchJoin()
                .where(
                        chatMessageJpaEntity.room.id.eq(roomId),
                        chatMessageJpaEntity.deletedAt.isNull(),
                        buildCursorCondition(lastCreatedAt, lastMessageId)
                )
                .orderBy(chatMessageJpaEntity.createdAt.desc(), chatMessageJpaEntity.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public void readMessage(Long roomId, Long lastMessageId, Long readerId) {
        long updated = queryFactory
                .update(chatMessageJpaEntity)
                .set(chatMessageJpaEntity.checked, true)
                .where(
                        chatMessageJpaEntity.room.id.eq(roomId),
                        chatMessageJpaEntity.deletedAt.isNull(),
                        chatMessageJpaEntity.checked.isFalse(),
                        chatMessageJpaEntity.id.loe(lastMessageId),
                        chatMessageJpaEntity.sender.id.ne(readerId)
                )
                .execute();

        em.flush();
        em.clear();
    }

    private BooleanExpression buildCursorCondition(LocalDateTime lastCreatedAt, Long lastMessageId) {
        if (lastCreatedAt == null || lastMessageId == null) {
            return null;
        }
        return chatMessageJpaEntity.createdAt.lt(lastCreatedAt)
                .or(chatMessageJpaEntity.createdAt.eq(lastCreatedAt)
                        .and(chatMessageJpaEntity.id.lt(lastMessageId)));
    }

}
