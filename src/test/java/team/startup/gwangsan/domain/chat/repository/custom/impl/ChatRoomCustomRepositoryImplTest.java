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
        repository = new ChatRoomCustomRepositoryImpl(new JPAQueryFactory(em.getEntityManager()));
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
    }
}
