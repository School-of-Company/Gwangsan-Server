package team.startup.gwangsan.domain.chat.repository.custom.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.QChatMessage;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomsDto;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomMemberResponse;
import team.startup.gwangsan.domain.chat.repository.custom.ChatRoomCustomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.QMember;

import java.util.List;
import java.util.Optional;

import static team.startup.gwangsan.domain.chat.entity.QChatMessage.chatMessage;
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
                        .fetchFirst()
        );
    }

    @Override
    public List<GetRoomsDto> findRoomsByMemberId(Long memberId) {
        QChatMessage latestMessage = QChatMessage.chatMessage;
        QChatMessage subMessage = new QChatMessage("subMessage");
        QChatMessage unreadMessage = new QChatMessage("unreadMessage");
        QMember member1 = new QMember("member1");
        QMember member2 = new QMember("member2");

        JPQLQuery<Long> latestMessageIdSubQuery = JPAExpressions
                .select(subMessage.id.max())
                .from(subMessage)
                .where(subMessage.createdAt.eq(
                        JPAExpressions
                                .select(chatMessage.createdAt.max())
                                .from(chatMessage)
                                .where(chatMessage.room.eq(chatRoom))
                ).and(subMessage.room.eq(chatRoom)));

        JPQLQuery<Long> unreadCountSubQuery = JPAExpressions
                .select(unreadMessage.count())
                .from(unreadMessage)
                .where(unreadMessage.room.eq(chatRoom)
                        .and(unreadMessage.sender.id.ne(memberId))
                        .and(unreadMessage.checked.isFalse()));

        return queryFactory
                .select(Projections.constructor(GetRoomsDto.class,
                        chatRoom.id,
                        Projections.constructor(GetRoomMemberResponse.class,
                                new CaseBuilder()
                                        .when(chatRoom.member1.id.eq(memberId))
                                        .then(chatRoom.member2.id)
                                        .otherwise(chatRoom.member1.id),
                                new CaseBuilder()
                                        .when(chatRoom.member1.id.eq(memberId))
                                        .then(chatRoom.member2.nickname)
                                        .otherwise(chatRoom.member1.nickname)),
                        latestMessage.id,
                        latestMessage.content,
                        latestMessage.messageType,
                        latestMessage.createdAt,
                        unreadCountSubQuery,
                        chatRoom.product.id
                )).distinct()
                .from(chatRoom)
                .leftJoin(latestMessage)
                .on(latestMessage.id.eq(latestMessageIdSubQuery))
                .join(chatRoom.member1, member1)
                .join(chatRoom.member2, member2)
                .where(chatRoom.isActive.isTrue()
                        .and(chatRoom.member1.id.eq(memberId).or(chatRoom.member2.id.eq(memberId))))
                .orderBy(latestMessage.createdAt.desc().nullsLast())
                .groupBy(chatRoom.id)
                .fetch();
    }
}
