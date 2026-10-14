package team.startup.gwangsan.adapter.persistence.chat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static team.startup.gwangsan.adapter.persistence.chat.QChatMessageImageJpaEntity.chatMessageImageJpaEntity;

@Repository
@RequiredArgsConstructor
public class ChatMessageImageCustomRepositoryImpl implements ChatMessageImageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatMessageImageJpaEntity> findAllByChatMessageIdIn(List<Long> chatMessageId) {
        return queryFactory
                .selectFrom(chatMessageImageJpaEntity)
                .join(chatMessageImageJpaEntity.image).fetchJoin()
                .where(chatMessageImageJpaEntity.chatMessage.id.in(chatMessageId))
                .fetch();
    }
}
