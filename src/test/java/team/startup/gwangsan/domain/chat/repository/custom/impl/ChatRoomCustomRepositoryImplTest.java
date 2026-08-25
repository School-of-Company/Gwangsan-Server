package team.startup.gwangsan.domain.chat.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomsDto;
import team.startup.gwangsan.domain.chat.repository.projection.LatestMessageDto;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;

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

    private Member createMember(String nickname, String phone) {
        return em.persist(Member.builder()
                .name("테스트")
                .nickname(nickname)
                .password("pw")
                .phoneNumber(phone)
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    private Product createProduct(Member owner) {
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

    private ChatRoom createRoom(Member buyer, Member seller, Product product) {
        return em.persist(ChatRoom.builder()
                .isActive(true)
                .buyer(buyer)
                .seller(seller)
                .product(product)
                .build());
    }

    private void createMessage(Long id, ChatRoom room, Member sender, String content, LocalDateTime createdAt) {
        em.persist(ChatMessage.builder()
                .id(id)
                .content(content)
                .messageType(MessageType.TEXT)
                .checked(false)
                .room(room)
                .sender(sender)
                .createdAt(createdAt)
                .build());
    }

    @Nested
    @DisplayName("findRoomsByMemberId()는")
    class Describe_findRoomsByMemberId {

        @Test
        @DisplayName("createdAt이 최신인데 id가 더 작은 메시지를 최신 메시지로 반환한다")
        void it_selects_message_with_latest_createdAt_even_if_id_is_smaller() {
            Member buyer = createMember("buyer1", "010-0001-0001");
            Member seller = createMember("seller1", "010-0001-0002");
            ChatRoom room = createRoom(buyer, seller, createProduct(seller));

            LocalDateTime older = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime newer = LocalDateTime.of(2024, 1, 1, 10, 1);

            createMessage(100L, room, seller, "오래된 메시지", older);   // id 높음, 시간 오래됨
            createMessage(90L, room, buyer, "최신 메시지", newer);       // id 낮음, 시간 최신

            em.flush();
            em.getEntityManager().clear();

            List<GetRoomsDto> result = repository.findRoomsByMemberId(buyer.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).messageId()).isEqualTo(90L);
            assertThat(result.get(0).lastMessage()).isEqualTo("최신 메시지");
        }

        @Test
        @DisplayName("createdAt이 같을 때 id가 더 큰 메시지를 최신 메시지로 반환한다")
        void it_selects_message_with_larger_id_when_createdAt_is_same() {
            Member buyer = createMember("buyer2", "010-0002-0001");
            Member seller = createMember("seller2", "010-0002-0002");
            ChatRoom room = createRoom(buyer, seller, createProduct(seller));

            LocalDateTime sameTime = LocalDateTime.of(2024, 1, 1, 10, 0);

            createMessage(10L, room, seller, "먼저 온 메시지", sameTime);
            createMessage(20L, room, buyer, "나중에 온 메시지", sameTime);

            em.flush();
            em.getEntityManager().clear();

            List<GetRoomsDto> result = repository.findRoomsByMemberId(buyer.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).messageId()).isEqualTo(20L);
            assertThat(result.get(0).lastMessage()).isEqualTo("나중에 온 메시지");
        }

        @Test
        @DisplayName("요청자 쪽에서 숨김 처리한 채팅방은 결과에서 제외한다")
        void it_excludes_room_hidden_by_requester() {
            Member buyer = createMember("buyer3", "010-0003-0001");
            Member seller = createMember("seller3", "010-0003-0002");
            ChatRoom room = createRoom(buyer, seller, createProduct(seller));
            room.hideFor(buyer, LocalDateTime.of(2024, 1, 1, 0, 0));
            em.persist(room);

            em.flush();
            em.getEntityManager().clear();

            assertThat(repository.findRoomsByMemberId(buyer.getId())).isEmpty();
        }

        @Test
        @DisplayName("상대방이 숨김 처리해도 요청자의 결과에는 그대로 노출된다")
        void it_keeps_room_visible_for_the_other_participant() {
            Member buyer = createMember("buyer4", "010-0004-0001");
            Member seller = createMember("seller4", "010-0004-0002");
            ChatRoom room = createRoom(buyer, seller, createProduct(seller));
            room.hideFor(buyer, LocalDateTime.of(2024, 1, 1, 0, 0));
            em.persist(room);

            em.flush();
            em.getEntityManager().clear();

            assertThat(repository.findRoomsByMemberId(seller.getId())).hasSize(1);
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
