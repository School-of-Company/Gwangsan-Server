package team.startup.gwangsan.domain.chat.repository.custom.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.QChatMessage;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomsDto;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomMemberResponse;
import team.startup.gwangsan.domain.chat.repository.custom.ChatRoomCustomRepository;
import team.startup.gwangsan.domain.chat.repository.projection.ChatRoomDto;
import team.startup.gwangsan.domain.chat.repository.projection.LatestMessageDto;
import team.startup.gwangsan.domain.chat.repository.projection.UnreadCountDto;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.QMember;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        QChatMessage message = QChatMessage.chatMessage;
        QChatMessage unreadMessage = new QChatMessage("unreadMessage");
        QMember buyer = new QMember("buyer");
        QMember seller = new QMember("seller");

        List<ChatRoomDto> rooms = queryFactory
                .select(Projections.constructor(ChatRoomDto.class,
                        chatRoom.id,
                        new CaseBuilder()
                                .when(chatRoom.buyer.id.eq(memberId))
                                .then(chatRoom.seller.id)
                                .otherwise(chatRoom.buyer.id),
                        new CaseBuilder()
                                .when(chatRoom.buyer.id.eq(memberId))
                                .then(chatRoom.seller.nickname)
                                .otherwise(chatRoom.buyer.nickname),
                        chatRoom.product.id
                ))
                .from(chatRoom)
                .join(chatRoom.buyer, buyer)
                .join(chatRoom.seller, seller)
                .where(chatRoom.isActive.isTrue()
                        .and(chatRoom.buyer.id.eq(memberId).or(chatRoom.seller.id.eq(memberId))))
                .fetch();

        if (rooms.isEmpty()) {
            return List.of();
        }

        List<Long> roomIds = rooms.stream()
                .map(ChatRoomDto::roomId)
                .toList();

        Map<Long, ChatRoomDto> roomMap = rooms.stream()
                .collect(Collectors.toMap(ChatRoomDto::roomId, r -> r));

        List<LatestMessageDto> latestMessages = queryFactory
                .select(Projections.constructor(LatestMessageDto.class,
                        message.room.id,
                        message.id,
                        message.content,
                        message.messageType,
                        message.createdAt
                ))
                .from(message)
                .where(message.room.id.in(roomIds))
                .orderBy(message.room.id.asc(),
                        message.createdAt.desc(),
                        message.id.desc())
                .fetch();

        Map<Long, LatestMessageDto> latestMessageMap = new LinkedHashMap<>();
        for (LatestMessageDto lm : latestMessages) {
            latestMessageMap.putIfAbsent(lm.roomId(), lm);
        }

        List<UnreadCountDto> unreadCounts = queryFactory
                .select(Projections.constructor(UnreadCountDto.class,
                        unreadMessage.room.id,
                        unreadMessage.count()
                ))
                .from(unreadMessage)
                .where(unreadMessage.room.id.in(roomIds)
                        .and(unreadMessage.sender.id.ne(memberId))
                        .and(unreadMessage.checked.isFalse()))
                .groupBy(unreadMessage.room.id)
                .fetch();

        Map<Long, Long> unreadCountMap = unreadCounts.stream()
                .collect(Collectors.toMap(UnreadCountDto::roomId, UnreadCountDto::unreadCount));

        List<Long> orderedRoomIds = roomIds.stream()
                .sorted(Comparator.comparing(
                        (Long roomId) -> {
                            LatestMessageDto lm = latestMessageMap.get(roomId);
                            return lm != null ? lm.createdAt() : LocalDateTime.MIN;
                        }
                ).reversed())
                .toList();

        List<GetRoomsDto> result = new java.util.ArrayList<>();

        for (Long roomId : orderedRoomIds) {
            ChatRoomDto room = roomMap.get(roomId);

            LatestMessageDto lm = latestMessageMap.get(roomId);
            Long unreadCount = unreadCountMap.getOrDefault(roomId, 0L);

            GetRoomMemberResponse memberResponse =
                    new GetRoomMemberResponse(room.opponentId(), room.opponentNickname());

            GetRoomsDto dto = new GetRoomsDto(
                    room.roomId(),
                    memberResponse,
                    lm != null ? lm.messageId() : null,
                    lm != null ? lm.content() : null,
                    lm != null ? lm.messageType() : null,
                    lm != null ? lm.createdAt() : null,
                    unreadCount,
                    room.productId()
            );

            result.add(dto);
        }

        return result;
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
