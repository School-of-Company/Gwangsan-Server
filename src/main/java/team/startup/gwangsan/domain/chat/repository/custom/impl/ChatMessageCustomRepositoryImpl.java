package team.startup.gwangsan.domain.chat.repository.custom.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.repository.custom.ChatMessageCustomRepository;

import java.time.LocalDateTime;
import java.util.List;

import static team.startup.gwangsan.domain.chat.entity.QChatMessage.chatMessage;
import static team.startup.gwangsan.domain.member.entity.QMember.member;

@Repository
@RequiredArgsConstructor
public class ChatMessageCustomRepositoryImpl implements ChatMessageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatMessage> findChatMessageByRoomIdsWithCursorPaging(List<Long> roomIds, LocalDateTime lastCreatedAt, Long lastMessageId, int limit) {
        return queryFactory
                .selectFrom(chatMessage)
                .join(chatMessage.sender, member).fetchJoin()
                .where(
                        chatMessage.room.id.in(roomIds),
                        buildCursorCondition(lastCreatedAt, lastMessageId)
                )
                .orderBy(chatMessage.createdAt.desc(), chatMessage.id.desc())
                .limit(limit)
                .fetch();
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
