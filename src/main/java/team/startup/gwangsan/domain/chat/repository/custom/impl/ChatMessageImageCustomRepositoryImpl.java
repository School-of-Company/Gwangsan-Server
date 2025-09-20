package team.startup.gwangsan.domain.chat.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.chat.entity.ChatMessageImage;
import team.startup.gwangsan.domain.chat.repository.custom.ChatMessageImageCustomRepository;

import java.util.List;

import static team.startup.gwangsan.domain.chat.entity.QChatMessageImage.chatMessageImage;

@Repository
@RequiredArgsConstructor
public class ChatMessageImageCustomRepositoryImpl implements ChatMessageImageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatMessageImage> findAllByChatMessageIdIn(List<Long> chatMessageId) {
        return queryFactory
                .selectFrom(chatMessageImage)
                .join(chatMessageImage.image).fetchJoin()
                .where(chatMessageImage.chatMessage.id.in(chatMessageId))
                .fetch();
    }
}
