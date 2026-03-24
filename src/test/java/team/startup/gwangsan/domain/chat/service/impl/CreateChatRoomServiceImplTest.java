package team.startup.gwangsan.domain.chat.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.block.exception.BlockedMemberException;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.presentation.dto.response.CreateChatRoomResponse;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.util.BlockValidator;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateChatRoomServiceImpl 단위 테스트")
class CreateChatRoomServiceImplTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private ProductRepository productRepository;
    @Mock private BlockValidator blockValidator;

    @InjectMocks
    private CreateChatRoomServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        private Member currentMember;
        private Member productOwner;
        private Product product;

        @BeforeEach
        void setUp() {
            currentMember = mock(Member.class);
            productOwner = mock(Member.class);
            product = mock(Product.class);
            when(memberUtil.getCurrentMember()).thenReturn(currentMember);
        }

        private void arrangeDefaultScenario() {
            when(product.getMember()).thenReturn(productOwner);
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            doNothing().when(blockValidator).validate(any(Member.class), any(Member.class));
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 NotFoundProductException 을 던진다")
        void it_throws_NotFoundProductException_when_product_not_found() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(99L))
                    .isInstanceOf(NotFoundProductException.class);
        }

        @Test
        @DisplayName("차단 관계이면 BlockedMemberException 을 던진다")
        void it_throws_BlockedMemberException_when_blocked() {
            when(product.getMember()).thenReturn(productOwner);
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            doThrow(new BlockedMemberException()).when(blockValidator).validate(any(Member.class), any(Member.class));

            assertThatThrownBy(() -> service.execute(10L))
                    .isInstanceOf(BlockedMemberException.class);
        }

        @Test
        @DisplayName("이미 채팅방이 존재하면 기존 방 ID 를 반환하고 새로 저장하지 않는다")
        void it_returns_existing_room_id_without_saving() {
            arrangeDefaultScenario();
            when(product.getMode()).thenReturn(Mode.GIVER);
            ChatRoom existingRoom = mock(ChatRoom.class);
            when(existingRoom.getId()).thenReturn(100L);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(eq(10L), eq(currentMember), eq(productOwner)))
                    .thenReturn(Optional.of(existingRoom));

            CreateChatRoomResponse response = service.execute(10L);

            assertThat(response.roomId()).isEqualTo(100L);
            verify(chatRoomRepository, never()).save(any());
        }

        @Test
        @DisplayName("GIVER 모드이면 현재 사용자가 buyer, 상품주가 seller 로 방을 생성한다")
        void it_creates_room_with_current_user_as_buyer_when_mode_is_giver() {
            arrangeDefaultScenario();
            when(product.getMode()).thenReturn(Mode.GIVER);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(eq(10L), eq(currentMember), eq(productOwner)))
                    .thenReturn(Optional.empty());
            ChatRoom savedRoom = mock(ChatRoom.class);
            when(savedRoom.getId()).thenReturn(200L);
            when(chatRoomRepository.save(any())).thenReturn(savedRoom);

            CreateChatRoomResponse response = service.execute(10L);

            assertThat(response.roomId()).isEqualTo(200L);
            ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(captor.capture());
            assertThat(captor.getValue().getBuyer()).isSameAs(currentMember);
            assertThat(captor.getValue().getSeller()).isSameAs(productOwner);
        }

        @Test
        @DisplayName("RECEIVER 모드이면 상품주가 buyer, 현재 사용자가 seller 로 방을 생성한다")
        void it_creates_room_with_product_owner_as_buyer_when_mode_is_receiver() {
            arrangeDefaultScenario();
            when(product.getMode()).thenReturn(Mode.RECEIVER);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(eq(10L), eq(productOwner), eq(currentMember)))
                    .thenReturn(Optional.empty());
            ChatRoom savedRoom = mock(ChatRoom.class);
            when(savedRoom.getId()).thenReturn(300L);
            when(chatRoomRepository.save(any())).thenReturn(savedRoom);

            CreateChatRoomResponse response = service.execute(10L);

            assertThat(response.roomId()).isEqualTo(300L);
            ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(captor.capture());
            assertThat(captor.getValue().getBuyer()).isSameAs(productOwner);
            assertThat(captor.getValue().getSeller()).isSameAs(currentMember);
        }

        @Test
        @DisplayName("현재 사용자가 상품 등록자와 동일하면 buyer 와 seller 가 같은 채팅방이 생성된다")
        void it_creates_room_where_buyer_and_seller_are_same_when_self_product() {
            when(product.getMember()).thenReturn(currentMember);
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            doNothing().when(blockValidator).validate(any(Member.class), any(Member.class));
            when(product.getMode()).thenReturn(Mode.GIVER);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(eq(10L), eq(currentMember), eq(currentMember)))
                    .thenReturn(Optional.empty());
            ChatRoom savedRoom = mock(ChatRoom.class);
            when(savedRoom.getId()).thenReturn(400L);
            when(chatRoomRepository.save(any())).thenReturn(savedRoom);

            CreateChatRoomResponse response = service.execute(10L);

            ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(captor.capture());
            assertThat(captor.getValue().getBuyer()).isSameAs(captor.getValue().getSeller());
        }
    }
}
