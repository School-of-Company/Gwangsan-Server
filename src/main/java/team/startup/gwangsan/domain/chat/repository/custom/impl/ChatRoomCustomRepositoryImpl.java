package team.startup.gwangsan.domain.chat.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.repository.custom.ChatRoomCustomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.QMember;

import java.util.Optional;

import static team.startup.gwangsan.domain.chat.entity.QChatRoom.chatRoom;

@Repository
@RequiredArgsConstructor
public class ChatRoomCustomRepositoryImpl implements ChatRoomCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<ChatRoom> findChatRoomByRoomId(Long roomId) {
        QMember member1 = new QMember("member1");
        QMember member2 = new QMember("member2");

        return Optional.ofNullable(queryFactory
                .selectFrom(chatRoom)
                .join(chatRoom.member1, member1).fetchJoin()
                .join(chatRoom.member2, member2).fetchJoin()
                .where(chatRoom.id.eq(roomId))
                .fetchOne());
    }

    @Override
    public Optional<ChatRoom> findByProductIdAndMember(Long productId, Member member) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(chatRoom)
                        .where(
                                chatRoom.product.id.eq(productId),
                                chatRoom.member1.eq(member).or(chatRoom.member2.eq(member))
                        )
                        .fetchOne()
        );
    }
}
