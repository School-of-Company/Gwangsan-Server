package team.startup.gwangsan.adapter.persistence.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, ChatMemberCleanupPersistenceAdapter.class})
@DisplayName("ChatMemberCleanupPersistenceAdapter 통합 테스트")
class ChatMemberCleanupPersistenceAdapterTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ChatMemberCleanupPersistenceAdapter adapter;

    @Nested
    @DisplayName("reassignMember()는")
    class Describe_reassignMember {

        @Test
        @DisplayName("it_탈퇴 회원의 채팅방 구매자·판매자와 메시지 발신자만 대체 회원으로 바꾼다")
        void it_reassigns_only_target_references() {
            MemberJpaEntity target = member("target", "010-4001-0001");
            MemberJpaEntity other = member("other", "010-4001-0002");
            MemberJpaEntity dummy = member("dummy", "010-4001-0003");
            Product product = product(other);
            ChatRoomJpaEntity asBuyer = room(target, other, product);
            ChatRoomJpaEntity asSeller = room(other, target, product);
            message(1L, asBuyer, target);
            message(2L, asBuyer, other);
            em.flush();

            adapter.reassignMember(target.getId(), dummy.getId());
            em.clear();

            ChatRoomJpaEntity buyerRoom = em.find(ChatRoomJpaEntity.class, asBuyer.getId());
            ChatRoomJpaEntity sellerRoom = em.find(ChatRoomJpaEntity.class, asSeller.getId());
            assertThat(buyerRoom.getBuyer().getId()).isEqualTo(dummy.getId());
            assertThat(buyerRoom.getSeller().getId()).isEqualTo(other.getId());
            assertThat(sellerRoom.getBuyer().getId()).isEqualTo(other.getId());
            assertThat(sellerRoom.getSeller().getId()).isEqualTo(dummy.getId());
            assertThat(em.find(ChatMessageJpaEntity.class, 1L).getSender().getId()).isEqualTo(dummy.getId());
            assertThat(em.find(ChatMessageJpaEntity.class, 2L).getSender().getId()).isEqualTo(other.getId());
        }
    }

    private MemberJpaEntity member(String nickname, String phone) {
        return em.persist(MemberJpaEntity.builder()
                .name("테스트")
                .nickname(nickname)
                .password("pw")
                .phoneNumber(phone)
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    private Product product(MemberJpaEntity owner) {
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

    private ChatRoomJpaEntity room(MemberJpaEntity buyer, MemberJpaEntity seller, Product product) {
        return em.persist(ChatRoomJpaEntity.builder()
                .isActive(true)
                .buyer(buyer)
                .seller(seller)
                .product(product)
                .build());
    }

    private void message(Long id, ChatRoomJpaEntity room, MemberJpaEntity sender) {
        em.persist(ChatMessageJpaEntity.builder()
                .id(id)
                .content("메시지")
                .messageType(MessageType.TEXT)
                .checked(false)
                .room(room)
                .sender(sender)
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build());
    }
}
