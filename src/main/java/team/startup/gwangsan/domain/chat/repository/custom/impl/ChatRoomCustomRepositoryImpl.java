package team.startup.gwangsan.domain.chat.repository.custom.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomsDto;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomMemberResponse;
import team.startup.gwangsan.domain.chat.repository.custom.ChatRoomCustomRepository;
import team.startup.gwangsan.domain.member.entity.Member;

import java.util.List;
import java.util.Optional;

import team.startup.gwangsan.domain.chat.entity.QChatMessage;
import team.startup.gwangsan.domain.member.entity.QMember;
import static team.startup.gwangsan.domain.chat.entity.QChatMessage.chatMessage;
import static team.startup.gwangsan.domain.chat.entity.QChatRoom.chatRoom;

@Repository
@RequiredArgsConstructor
public class ChatRoomCustomRepositoryImpl implements ChatRoomCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<ChatRoom> findChatRoomByRoomId(Long roomId) {
        QMember buyer = new QMember("buyer");
        QMember seller = new QMember("seller");

        return Optional.ofNullable(queryFactory
                .selectFrom(chatRoom)
                .join(chatRoom.buyer, buyer).fetchJoin()
                .join(chatRoom.seller, seller).fetchJoin()
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
                                chatRoom.buyer.eq(member).or(chatRoom.seller.eq(member))
                        )
                        .fetchFirst()
        );
    }

    @Override
    public List<GetRoomsDto> findRoomsByMemberId(Long memberId) {
        QChatMessage latestMessage = QChatMessage.chatMessage;
        QChatMessage subMessage = new QChatMessage("subMessage");
        QChatMessage unreadMessage = new QChatMessage("unreadMessage");
        QMember buyer = new QMember("buyer");
        QMember seller = new QMember("seller");

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
                                        .when(chatRoom.buyer.id.eq(memberId))
                                        .then(chatRoom.seller.id)
                                        .otherwise(chatRoom.buyer.id),
                                new CaseBuilder()
                                        .when(chatRoom.buyer.id.eq(memberId))
                                        .then(chatRoom.seller.nickname)
                                        .otherwise(chatRoom.buyer.nickname)),
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
                .join(chatRoom.buyer, buyer)
                .join(chatRoom.seller, seller)
                .where(chatRoom.isActive.isTrue()
                        .and(chatRoom.buyer.id.eq(memberId).or(chatRoom.seller.id.eq(memberId))))
                .orderBy(latestMessage.createdAt.desc().nullsLast())
                .groupBy(chatRoom.id)
                .fetch();
    }

    @Override
    public Optional<ChatRoom> findByRoomIdWithSellerAndProduct(Long roomId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(chatRoom)
                        .join(chatRoom.seller).fetchJoin()
                        .join(chatRoom.product).fetchJoin()
                        .where(chatRoom.id.eq(roomId))
                        .fetchFirst()
        );
    }
}
