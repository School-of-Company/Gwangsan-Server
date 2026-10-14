package team.startup.gwangsan.adapter.persistence.chat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;
import team.startup.gwangsan.adapter.persistence.chat.ChatMessageJpaEntity;
import team.startup.gwangsan.adapter.persistence.chat.ChatRoomJpaEntity;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.application.chat.data.ChatRoomListRow;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("ChatRoomCustomRepositoryImpl 통합 테스트")
class ChatRoomCustomRepositoryImplTest {

    @Autowired
    private TestEntityManager em;

    private ChatRoomCustomRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ChatRoomCustomRepositoryImpl(new JPAQueryFactory(em.getEntityManager()), em.getEntityManager());
    }

    @Nested
    @DisplayName("채팅방 상세 작성자 ID 조회는")
    class Describe_findByRoomIdWithSellerAndProduct {
        @ParameterizedTest
        @EnumSource(Mode.class)
        @DisplayName("두 Mode에서 작성자 ID 접근으로 추가 SQL을 실행하지 않는다")
        void it_reads_author_id_without_an_extra_query(Mode mode) {
            MemberJpaEntity owner = createMember("owner", "01011111111");
            MemberJpaEntity partner = createMember("partner", "01022222222");
            Product product = createProduct(owner);
            product.update(Type.SERVICE, mode, "상품", "설명", 5000);
            ChatRoomJpaEntity room = createRoom(mode == Mode.GIVER ? partner : owner,
                    mode == Mode.GIVER ? owner : partner, product);
            Long roomId = room.getId();
            Long ownerId = owner.getId();
            em.flush();
            em.clear();
            var statistics = em.getEntityManager().getEntityManagerFactory()
                    .unwrap(SessionFactory.class).getStatistics();
            boolean wasEnabled = statistics.isStatisticsEnabled();
            statistics.setStatisticsEnabled(true);
            try {
                ChatRoomJpaEntity loaded = repository.findByRoomIdWithSellerAndProduct(roomId).orElseThrow();
                long statements = statistics.getPrepareStatementCount();
                assertThat(loaded.getProduct().getMember().getId()).isEqualTo(ownerId);
                assertThat(statistics.getPrepareStatementCount()).isEqualTo(statements);
            } finally {
                statistics.setStatisticsEnabled(wasEnabled);
            }
        }
    }

    private MemberJpaEntity createMember(String nickname, String phone) {
        return em.persist(MemberJpaEntity.builder()
                .name("테스트")
                .nickname(nickname)
                .password("pw")
                .phoneNumber(phone)
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    private Product createProduct(MemberJpaEntity owner) {
        return em.persist(Product.builder()
                .title("상품")
                .description("설명")
                .gwangsan(5000)
                .member(owner)
                .type(Type.SERVICE)
                .mode(Mode.GIVER)
                .status(ProductStatus.ONGOING)
                .build());
    }

    private ChatRoomJpaEntity createRoom(MemberJpaEntity buyer, MemberJpaEntity seller, Product product) {
        return createRoom(true, buyer, seller, product);
    }

    private ChatRoomJpaEntity createRoom(boolean active, MemberJpaEntity buyer, MemberJpaEntity seller, Product product) {
        return em.persist(ChatRoomJpaEntity.builder()
                .isActive(active)
                .buyer(buyer)
                .seller(seller)
                .product(product)
                .build());
    }

    private void createMessage(Long id, ChatRoomJpaEntity room, MemberJpaEntity sender, String content, LocalDateTime createdAt) {
        createMessage(id, room, sender, content, createdAt, false);
    }

    private void createMessage(Long id, ChatRoomJpaEntity room, MemberJpaEntity sender, String content, LocalDateTime createdAt,
                               boolean checked) {
        em.persist(ChatMessageJpaEntity.builder()
                .id(id)
                .content(content)
                .messageType(MessageType.TEXT)
                .checked(checked)
                .room(room)
                .sender(sender)
                .createdAt(createdAt)
                .build());
    }

    @Nested
    @DisplayName("findRoomsByMemberId()는")
    class Describe_findRoomsByMemberId {

        @Test
        void systemMessageDoesNotChangePreviewOrUnreadCount() {
            MemberJpaEntity buyer = createMember("system-buyer", "010-3001-0001");
            MemberJpaEntity seller = createMember("system-seller", "010-3001-0002");
            ChatRoomJpaEntity room = createRoom(buyer, seller, createProduct(seller));
            LocalDateTime older = LocalDateTime.of(2026, 9, 30, 10, 0);
            createMessage(100L, room, seller, "일반 메시지", older);
            em.persist(ChatMessageJpaEntity.builder().id(-1L).room(room).sender(seller)
                    .content("예약 취소").messageType(MessageType.SYSTEM).checked(false)
                    .createdAt(older.plusMinutes(1)).build());
            em.flush();
            em.clear();

            ChatRoomListRow result = repository.findRoomsByMemberId(buyer.getId()).getFirst();

            assertThat(result.lastMessage()).isEqualTo("일반 메시지");
            assertThat(result.unreadMessageCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("createdAt이 최신인데 id가 더 작은 메시지를 최신 메시지로 반환한다")
        void it_selects_message_with_latest_createdAt_even_if_id_is_smaller() {
            MemberJpaEntity buyer = createMember("buyer1", "010-0001-0001");
            MemberJpaEntity seller = createMember("seller1", "010-0001-0002");
            ChatRoomJpaEntity room = createRoom(buyer, seller, createProduct(seller));

            LocalDateTime older = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime newer = LocalDateTime.of(2024, 1, 1, 10, 1);

            createMessage(100L, room, seller, "오래된 메시지", older);   // id 높음, 시간 오래됨
            createMessage(90L, room, buyer, "최신 메시지", newer);       // id 낮음, 시간 최신

            em.flush();
            em.getEntityManager().clear();

            List<ChatRoomListRow> result = repository.findRoomsByMemberId(buyer.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).messageId()).isEqualTo(90L);
            assertThat(result.get(0).lastMessage()).isEqualTo("최신 메시지");
        }

        @Test
        @DisplayName("createdAt이 같을 때 id가 더 큰 메시지를 최신 메시지로 반환한다")
        void it_selects_message_with_larger_id_when_createdAt_is_same() {
            MemberJpaEntity buyer = createMember("buyer2", "010-0002-0001");
            MemberJpaEntity seller = createMember("seller2", "010-0002-0002");
            ChatRoomJpaEntity room = createRoom(buyer, seller, createProduct(seller));

            LocalDateTime sameTime = LocalDateTime.of(2024, 1, 1, 10, 0);

            createMessage(10L, room, seller, "먼저 온 메시지", sameTime);
            createMessage(20L, room, buyer, "나중에 온 메시지", sameTime);

            em.flush();
            em.getEntityManager().clear();

            List<ChatRoomListRow> result = repository.findRoomsByMemberId(buyer.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).messageId()).isEqualTo(20L);
            assertThat(result.get(0).lastMessage()).isEqualTo("나중에 온 메시지");
        }

        @Test
        @DisplayName("요청자 쪽에서 숨김 처리한 채팅방은 결과에서 제외한다")
        void it_excludes_room_hidden_by_requester() {
            MemberJpaEntity buyer = createMember("buyer3", "010-0003-0001");
            MemberJpaEntity seller = createMember("seller3", "010-0003-0002");
            ChatRoomJpaEntity room = createRoom(buyer, seller, createProduct(seller));
            room.updateHiddenState(LocalDateTime.of(2024, 1, 1, 0, 0), null);
            em.persist(room);

            em.flush();
            em.getEntityManager().clear();

            assertThat(repository.findRoomsByMemberId(buyer.getId())).isEmpty();
        }

        @Test
        @DisplayName("상대방이 숨김 처리해도 요청자의 결과에는 그대로 노출된다")
        void it_keeps_room_visible_for_the_other_participant() {
            MemberJpaEntity buyer = createMember("buyer4", "010-0004-0001");
            MemberJpaEntity seller = createMember("seller4", "010-0004-0002");
            ChatRoomJpaEntity room = createRoom(buyer, seller, createProduct(seller));
            room.updateHiddenState(LocalDateTime.of(2024, 1, 1, 0, 0), null);
            em.persist(room);

            em.flush();
            em.getEntityManager().clear();

            assertThat(repository.findRoomsByMemberId(seller.getId())).hasSize(1);
        }

        @Test
        @DisplayName("활성 채팅방만 최신 메시지와 상대방의 미확인 메시지 수를 함께 반환한다")
        void it_returns_active_rooms_with_latest_message_and_unread_count() {
            MemberJpaEntity buyer = createMember("buyer5", "010-0005-0001");
            MemberJpaEntity seller = createMember("seller5", "010-0005-0002");
            ChatRoomJpaEntity messageRoom = createRoom(buyer, seller, createProduct(seller));
            ChatRoomJpaEntity noMessageRoom = createRoom(buyer, seller, createProduct(seller));
            createRoom(false, buyer, seller, createProduct(seller));

            LocalDateTime sentAt = LocalDateTime.of(2024, 1, 1, 10, 0);
            createMessage(30L, messageRoom, seller, "읽지 않은 메시지", sentAt, false);
            createMessage(31L, messageRoom, seller, "읽은 메시지", sentAt.plusMinutes(1), true);
            createMessage(32L, messageRoom, buyer, "내 메시지", sentAt.plusMinutes(2), false);

            em.flush();
            em.clear();

            List<ChatRoomListRow> result = repository.findRoomsByMemberId(buyer.getId());

            assertThat(result).extracting(ChatRoomListRow::roomId)
                    .containsExactly(messageRoom.getId(), noMessageRoom.getId());
            assertThat(result.getFirst().opponentId()).isEqualTo(seller.getId());
            assertThat(result.getFirst())
                    .extracting(ChatRoomListRow::messageId, ChatRoomListRow::lastMessage, ChatRoomListRow::lastMessageTime,
                            ChatRoomListRow::unreadMessageCount)
                    .containsExactly(32L, "내 메시지", sentAt.plusMinutes(2), 1L);
            assertThat(result.get(1))
                    .extracting(ChatRoomListRow::messageId, ChatRoomListRow::lastMessage, ChatRoomListRow::lastMessageType,
                            ChatRoomListRow::lastMessageTime, ChatRoomListRow::unreadMessageCount)
                    .containsExactly(null, null, null, null, 0L);
        }

        @Test
        @DisplayName("참여하는 공개 활성 채팅방이 없으면 빈 목록을 반환한다")
        void it_returns_empty_when_member_has_no_visible_active_rooms() {
            MemberJpaEntity buyer = createMember("buyer6", "010-0006-0001");
            MemberJpaEntity seller = createMember("seller6", "010-0006-0002");
            MemberJpaEntity outsider = createMember("outsider6", "010-0006-0003");
            createRoom(buyer, seller, createProduct(seller));

            em.flush();
            em.clear();

            assertThat(repository.findRoomsByMemberId(outsider.getId())).isEmpty();
        }

    }

    @Nested
    @DisplayName("toLatestMessageDto()는")
    class Describe_toLatestMessageDto {

        @Test
        @DisplayName("message_type이 알 수 없는 값이면 예외 대신 null을 반환한다")
        void it_returns_null_when_message_type_is_unknown() {
            Object[] row = {1L, 100L, "손상된 메시지", "CORRUPTED_TYPE", LocalDateTime.of(2024, 1, 1, 10, 0)};

            LatestMessageDto result = repository.toLatestMessageDto(row);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("created_at이 지원하지 않는 타입이면 예외 대신 null을 반환한다")
        void it_returns_null_when_created_at_type_is_unsupported() {
            Object[] row = {1L, 100L, "메시지", "TEXT", "2024-01-01"};

            LatestMessageDto result = repository.toLatestMessageDto(row);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("정상적인 데이터면 LatestMessageDto를 반환한다")
        void it_returns_dto_when_row_is_valid() {
            LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
            Object[] row = {1L, 100L, "정상 메시지", "TEXT", createdAt};

            LatestMessageDto result = repository.toLatestMessageDto(row);

            assertThat(result.roomId()).isEqualTo(1L);
            assertThat(result.messageId()).isEqualTo(100L);
            assertThat(result.content()).isEqualTo("정상 메시지");
            assertThat(result.messageType()).isEqualTo(MessageType.TEXT);
            assertThat(result.createdAt()).isEqualTo(createdAt);
        }
    }
}
