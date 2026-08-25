package team.startup.gwangsan.domain.chat.repository.custom.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.QChatMessage;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomsDto;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomMemberResponse;
import team.startup.gwangsan.domain.chat.repository.custom.ChatRoomCustomRepository;
import team.startup.gwangsan.domain.chat.repository.projection.ChatRoomDto;
import team.startup.gwangsan.domain.chat.repository.projection.LatestMessageDto;
import team.startup.gwangsan.domain.chat.repository.projection.UnreadCountDto;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.QMember;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static team.startup.gwangsan.domain.chat.entity.QChatRoom.chatRoom;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatRoomCustomRepositoryImpl implements ChatRoomCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

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
                        .and(
                                chatRoom.buyer.id.eq(memberId).and(chatRoom.hiddenByBuyerAt.isNull())
                                        .or(chatRoom.seller.id.eq(memberId).and(chatRoom.hiddenBySellerAt.isNull()))
                        ))
                .fetch();

        if (rooms.isEmpty()) {
            return List.of();
        }

        List<Long> roomIds = rooms.stream()
                .map(ChatRoomDto::roomId)
                .toList();

        Map<Long, ChatRoomDto> roomMap = rooms.stream()
                .collect(Collectors.toMap(ChatRoomDto::roomId, r -> r));

        List<LatestMessageDto> latestMessages = findLatestMessagesByRoomIds(roomIds);

        Map<Long, LatestMessageDto> latestMessageMap = latestMessages.stream()
                .collect(Collectors.toMap(LatestMessageDto::roomId, lm -> lm, (existing, replacement) -> existing));

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

    private List<LatestMessageDto> findLatestMessagesByRoomIds(List<Long> roomIds) {
        String placeholders = roomIds.stream()
                .map(roomId -> "?")
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT room_id, message_id, content, message_type, created_at
                FROM (
                    SELECT
                        room_id,
                        message_id,
                        content,
                        message_type,
                        created_at,
                        ROW_NUMBER() OVER (
                            PARTITION BY room_id
                            ORDER BY created_at DESC, message_id DESC
                        ) AS row_num
                    FROM tbl_chat_message
                    WHERE room_id IN (%s)
                ) ranked_message
                WHERE row_num = 1
                """.formatted(placeholders);

        Query query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < roomIds.size(); i++) {
            query.setParameter(i + 1, roomIds.get(i));
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(this::toLatestMessageDto)
                .filter(Objects::nonNull)
                .toList();
    }

    LatestMessageDto toLatestMessageDto(Object[] row) {
        Long roomId = ((Number) row[0]).longValue();
        MessageType messageType = toMessageType(row[3]);
        LocalDateTime createdAt = toLocalDateTime(row[4]);

        if (messageType == null || createdAt == null) {
            log.warn("Skipping malformed chat message row for room {} (message_type={}, created_at={})",
                    roomId, row[3], row[4]);
            return null;
        }

        return new LatestMessageDto(
                roomId,
                ((Number) row[1]).longValue(),
                (String) row[2],
                messageType,
                createdAt
        );
    }

    private MessageType toMessageType(Object value) {
        try {
            return MessageType.valueOf(String.valueOf(value));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }
}
