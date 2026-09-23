package team.startup.gwangsan.domain.chat.repository.custom.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.repository.custom.ChatMessageCustomRepository;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static team.startup.gwangsan.domain.chat.entity.QChatMessage.chatMessage;
import static team.startup.gwangsan.domain.member.entity.QMember.member;

@Repository
@RequiredArgsConstructor
public class ChatMessageCustomRepositoryImpl implements ChatMessageCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean insertIfAbsent(ChatMessage message) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO tbl_chat_message
                        (message_id, room_id, sender_id, content, message_type, created_at, checked)
                    VALUES (:id, :roomId, :senderId, :content, :messageType, :createdAt, false)
                    """, new MapSqlParameterSource()
                    .addValue("id", message.getId())
                    .addValue("roomId", message.getRoom().getId())
                    .addValue("senderId", message.getSender().getId())
                    .addValue("content", message.getContent())
                    .addValue("messageType", message.getMessageType().name())
                    .addValue("createdAt", message.getCreatedAt()));
            return true;
        } catch (DuplicateKeyException e) {
            // Catch the JDBC statement failure here, before a transaction interceptor can mark rollback-only.
            if (isMariaDbDuplicate(e) && findStoredMessage(message.getId()).isPresent()) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public Optional<StoredMessage> findStoredMessage(Long messageId) {
        // A locking read sees the winner's commit even after an earlier REPEATABLE READ snapshot.
        return jdbcTemplate.query("""
                SELECT m.message_id, m.room_id, m.sender_id, m.content, m.message_type,
                       m.created_at, m.checked, i.image_id, i.image_url
                FROM tbl_chat_message m
                LEFT JOIN tbl_chat_message_image mi ON mi.message_id = m.message_id
                LEFT JOIN tbl_image i ON i.image_id = mi.image_id
                WHERE m.message_id = :messageId
                ORDER BY mi.id
                LOCK IN SHARE MODE
                """, Map.of("messageId", messageId), rs -> {
            if (!rs.next()) return Optional.empty();
            Long roomId = rs.getLong("room_id");
            Long senderId = rs.getObject("sender_id", Long.class);
            String content = rs.getString("content");
            MessageType messageType = MessageType.valueOf(rs.getString("message_type"));
            LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
            boolean checked = rs.getBoolean("checked");
            List<GetImageResponse> images = new ArrayList<>();
            do {
                Long imageId = rs.getObject("image_id", Long.class);
                if (imageId != null) {
                    images.add(new GetImageResponse(imageId, rs.getString("image_url")));
                }
            } while (rs.next());
            return Optional.of(new StoredMessage(messageId, roomId, senderId, content,
                    messageType, createdAt, checked, List.copyOf(images)));
        });
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
    public List<ChatMessage> findChatMessageByRoomIdWithCursorPaging(Long roomId, LocalDateTime lastCreatedAt, Long lastMessageId, int limit) {
        return queryFactory
                .selectFrom(chatMessage)
                .join(chatMessage.sender, member).fetchJoin()
                .where(
                        chatMessage.room.id.eq(roomId),
                        buildCursorCondition(lastCreatedAt, lastMessageId)
                )
                .orderBy(chatMessage.createdAt.desc(), chatMessage.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public void readMessage(Long roomId, Long lastMessageId, Long readerId) {
        long updated = queryFactory
                .update(chatMessage)
                .set(chatMessage.checked, true)
                .where(
                        chatMessage.room.id.eq(roomId),
                        chatMessage.checked.isFalse(),
                        chatMessage.id.loe(lastMessageId),
                        chatMessage.sender.id.ne(readerId)
                )
                .execute();

        em.flush();
        em.clear();
    }

    private BooleanExpression buildCursorCondition(LocalDateTime lastCreatedAt, Long lastMessageId) {
        if (lastCreatedAt == null || lastMessageId == null) {
            return null;
        }
        return chatMessage.createdAt.lt(lastCreatedAt)
                .or(chatMessage.createdAt.eq(lastCreatedAt)
                        .and(chatMessage.id.lt(lastMessageId)));
    }

}
