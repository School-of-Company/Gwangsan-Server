package team.startup.gwangsan.domain.chat.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatMessageImage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetChatMessagesResponse;
import team.startup.gwangsan.domain.chat.repository.ChatMessageImageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;
import team.startup.gwangsan.domain.trade.service.TradeStateReader;
import team.startup.gwangsan.domain.trade.service.TradeStateSnapshot;
import team.startup.gwangsan.global.util.MemberUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindChatMessageByRoomIdServiceImpl 단위 테스트")
class FindChatMessageByRoomIdServiceImplTest {

    private static final LocalDateTime TRADE_REQUESTED_AT = LocalDateTime.of(2026, 8, 30, 11, 20);

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatMessageImageRepository chatMessageImageRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private TradeStateReader tradeStateReader;
    @Mock private ProductReservationRepository productReservationRepository;

    @InjectMocks
    private FindChatMessageByRoomIdServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        private Member currentMember;
        private Member otherMember;
        private ChatRoom chatRoom;
        private Product product;

        @BeforeEach
        void setUp() {
            currentMember = mock(Member.class);
            otherMember = mock(Member.class);
            chatRoom = mock(ChatRoom.class);
            product = mock(Product.class);

            when(currentMember.getId()).thenReturn(1L);
            lenient().when(otherMember.getId()).thenReturn(2L);
            when(memberUtil.getCurrentMember()).thenReturn(currentMember);
        }

        // seller == currentMember 인 기본 happy path 공통 설정
        private void arrangeRoomAsSellerView() {
            when(chatRoom.getSeller()).thenReturn(currentMember);
            when(chatRoom.getBuyer()).thenReturn(otherMember);
            when(chatRoom.getProduct()).thenReturn(product);
            when(product.getId()).thenReturn(10L);
            when(product.getTitle()).thenReturn("상품명");
            when(chatRoomRepository.findByRoomIdWithSellerAndProduct(5L)).thenReturn(Optional.of(chatRoom));
            when(productImageRepository.findAllByProductId(10L)).thenReturn(List.of());
            givenTradeState(false, false, null);
        }

        // buyer == currentMember 인 happy path 공통 설정
        private void arrangeRoomAsBuyerView() {
            when(chatRoom.getSeller()).thenReturn(otherMember);
            when(chatRoom.getBuyer()).thenReturn(currentMember);
            when(chatRoom.getProduct()).thenReturn(product);
            when(product.getId()).thenReturn(10L);
            when(product.getTitle()).thenReturn("상품명");
            when(chatRoomRepository.findByRoomIdWithSellerAndProduct(5L)).thenReturn(Optional.of(chatRoom));
            when(productImageRepository.findAllByProductId(10L)).thenReturn(List.of());
            givenTradeState(false, false, null);
        }

        /** 거래 상태 스냅샷을 지정한다. isCompletable 은 실제 record 로직으로 계산된다. */
        private void givenTradeState(boolean completed, boolean reserved, Boolean requestedBySeller) {
            lenient().when(tradeStateReader.read(any(), any(), any()))
                    .thenReturn(new TradeStateSnapshot(completed, reserved, requestedBySeller, TRADE_REQUESTED_AT));
        }

        private void arrangeEmptyMessages() {
            when(chatMessageRepository.findChatMessageByRoomIdWithCursorPaging(eq(5L), any(), any(), anyInt()))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("채팅방이 없으면 NotFoundChatRoomException 을 던진다")
        void it_throws_NotFoundChatRoomException_when_room_not_found() {
            when(chatRoomRepository.findByRoomIdWithSellerAndProduct(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(99L, null, null, 20))
                    .isInstanceOf(NotFoundChatRoomException.class);
        }

        @Test
        @DisplayName("현재 사용자가 채팅방 멤버가 아니면 NotFoundChatRoomException 을 던진다")
        void it_throws_NotFoundChatRoomException_when_member_not_in_room() {
            Member stranger = mock(Member.class);
            when(stranger.getId()).thenReturn(99L);
            when(memberUtil.getCurrentMember()).thenReturn(stranger);
            when(chatRoom.getSeller()).thenReturn(currentMember);
            when(chatRoom.getBuyer()).thenReturn(otherMember);
            when(chatRoomRepository.findByRoomIdWithSellerAndProduct(5L)).thenReturn(Optional.of(chatRoom));

            assertThatThrownBy(() -> service.execute(5L, null, null, 20))
                    .isInstanceOf(NotFoundChatRoomException.class);
        }

        @Test
        @DisplayName("커서 파라미터가 repository 에 그대로 전달된다")
        void it_passes_cursor_params_to_repository() {
            arrangeRoomAsSellerView();
            LocalDateTime cursor = LocalDateTime.of(2024, 6, 1, 12, 0);
            when(chatMessageRepository.findChatMessageByRoomIdWithCursorPaging(5L, cursor, 42L, 10))
                    .thenReturn(List.of());

            service.execute(5L, cursor, 42L, 10);

            verify(chatMessageRepository).findChatMessageByRoomIdWithCursorPaging(5L, cursor, 42L, 10);
        }

        @Test
        @DisplayName("TEXT 메시지를 정상적으로 반환한다")
        void it_returns_text_messages_response() {
            arrangeRoomAsSellerView();
            ChatMessage msg = buildTextMessage(1L, "안녕");
            when(chatMessageRepository.findChatMessageByRoomIdWithCursorPaging(eq(5L), any(), any(), anyInt()))
                    .thenReturn(List.of(msg));

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.messages()).hasSize(1);
            assertThat(response.messages().get(0).content()).isEqualTo("안녕");
        }

        @Test
        @DisplayName("IMAGE 타입 메시지는 chatMessageImageRepository 에서 이미지를 조회하여 매핑한다")
        void it_maps_images_for_image_type_messages() {
            arrangeRoomAsSellerView();
            ChatMessage imageMsg = buildImageMessage(1L, "이미지");
            when(chatMessageRepository.findChatMessageByRoomIdWithCursorPaging(eq(5L), any(), any(), anyInt()))
                    .thenReturn(List.of(imageMsg));

            ChatMessageImage chatMessageImage = mock(ChatMessageImage.class);
            ChatMessage msgRef = mock(ChatMessage.class);
            Image image = mock(Image.class);
            when(msgRef.getId()).thenReturn(1L);
            when(chatMessageImage.getChatMessage()).thenReturn(msgRef);
            when(chatMessageImage.getImage()).thenReturn(image);
            when(image.getId()).thenReturn(100L);
            when(image.getImageUrl()).thenReturn("image-url");
            when(chatMessageImageRepository.findAllByChatMessageIdIn(List.of(1L)))
                    .thenReturn(List.of(chatMessageImage));

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.messages().get(0).images()).hasSize(1);
            assertThat(response.messages().get(0).images().get(0).imageUrl()).isEqualTo("image-url");
            verify(chatMessageImageRepository).findAllByChatMessageIdIn(List.of(1L));
        }

        @Test
        @DisplayName("TEXT 메시지만 있으면 chatMessageImageRepository 를 호출하지 않는다")
        void it_does_not_call_image_repo_when_no_image_messages() {
            arrangeRoomAsSellerView();
            ChatMessage textMsg = buildTextMessage(1L, "텍스트");
            when(chatMessageRepository.findChatMessageByRoomIdWithCursorPaging(eq(5L), any(), any(), anyInt()))
                    .thenReturn(List.of(textMsg));

            service.execute(5L, null, null, 20);

            verifyNoInteractions(chatMessageImageRepository);
        }

        @Test
        @DisplayName("seller 이면 isSeller 가 true 이다")
        void it_sets_isSeller_true_for_seller() {
            arrangeRoomAsSellerView();
            arrangeEmptyMessages();

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.product().isSeller()).isTrue();
        }

        @Test
        @DisplayName("buyer 이면 isSeller 가 false 이다")
        void it_sets_isSeller_false_for_buyer() {
            arrangeRoomAsBuyerView();
            arrangeEmptyMessages();

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.product().isSeller()).isFalse();
        }

        @Test
        @DisplayName("seller 이고 PENDING 요청이 없으면 isCompletable 이 true 이다 (판매자가 먼저 요청 가능)")
        void it_sets_isCompletable_true_when_seller_and_no_pending_request() {
            arrangeRoomAsSellerView();
            arrangeEmptyMessages();

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.product().isCompletable()).isTrue();
        }

        @Test
        @DisplayName("seller 본인이 이미 PENDING 요청을 보냈으면 isCompletable 이 false 이다 (상대 확정 대기)")
        void it_sets_isCompletable_false_when_seller_already_requested() {
            arrangeRoomAsSellerView();
            arrangeEmptyMessages();
            givenTradeState(false, false, true);

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.product().isCompletable()).isFalse();
        }

        @Test
        @DisplayName("buyer 이고 PENDING 요청이 없으면 isCompletable 이 true 이다 (구매자가 먼저 요청 가능)")
        void it_sets_isCompletable_true_when_buyer_and_no_pending_request() {
            arrangeRoomAsBuyerView();
            arrangeEmptyMessages();

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.product().isCompletable()).isTrue();
        }

        @Test
        @DisplayName("seller 가 PENDING 요청을 보냈으면 buyer 의 isCompletable 이 true 이다 (구매자가 확정 가능)")
        void it_sets_isCompletable_true_when_buyer_and_seller_requested() {
            arrangeRoomAsBuyerView();
            arrangeEmptyMessages();
            givenTradeState(false, false, true);

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.product().isCompletable()).isTrue();
        }

        @Test
        @DisplayName("buyer 본인이 이미 PENDING 요청을 보냈으면 isCompletable 이 false 이다 (상대 확정 대기)")
        void it_sets_isCompletable_false_when_buyer_already_requested() {
            arrangeRoomAsBuyerView();
            arrangeEmptyMessages();
            givenTradeState(false, false, false);

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.product().isCompletable()).isFalse();
        }

        @Test
        @DisplayName("상품이 이미 COMPLETED 상태이면 PENDING 요청이 없어도 isCompletable 이 false 이다")
        void it_sets_isCompletable_false_when_product_already_completed() {
            arrangeRoomAsSellerView();
            arrangeEmptyMessages();
            givenTradeState(true, false, null);

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.product().isCompletable()).isFalse();
            assertThat(response.product().isCompleted()).isTrue();
        }

        @Test
        @DisplayName("거래 요청 시각을 그대로 응답의 createdAt 으로 내려준다")
        void it_returns_trade_requested_at_as_created_at() {
            arrangeRoomAsSellerView();
            arrangeEmptyMessages();
            givenTradeState(false, false, true);

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            // 상품 생성 시각이 아니라 거래 요청 시각이며, 거래 상태 변경 이벤트와 같은 값이다.
            assertThat(response.product().createdAt()).isEqualTo(TRADE_REQUESTED_AT);
        }

        @Test
        @DisplayName("거래가 완료된 뒤에도 createdAt 이 남아 거래 카드가 유지된다")
        void it_keeps_created_at_after_trade_completion() {
            arrangeRoomAsSellerView();
            arrangeEmptyMessages();
            givenTradeState(true, false, null);

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            // null 이 되면 클라이언트가 거래 카드를 숨겨 완료 표시와 리뷰 작성 진입점이 사라진다.
            assertThat(response.product().createdAt()).isEqualTo(TRADE_REQUESTED_AT);
            assertThat(response.product().isCompleted()).isTrue();
        }

        @Test
        @DisplayName("철회가 승인되면 createdAt 이 null 이 되어 다시 거래를 요청할 수 있다")
        void it_clears_created_at_after_rollback() {
            arrangeRoomAsSellerView();
            arrangeEmptyMessages();
            lenient().when(tradeStateReader.read(any(), any(), any()))
                    .thenReturn(new TradeStateSnapshot(false, false, null, null));

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.product().createdAt()).isNull();
            assertThat(response.product().isCompletable()).isTrue();
        }

        @Test
        @DisplayName("상품이 예약 상태이면 isReserved 가 true 이다")
        void it_sets_isReserved_true_when_product_is_reserved() {
            arrangeRoomAsSellerView();
            arrangeEmptyMessages();
            givenTradeState(false, true, null);
            when(productReservationRepository.findByProductAndStatus(product, ReservationStatus.PENDING))
                    .thenReturn(Optional.empty());

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.product().isReserved()).isTrue();
        }

        @Test
        @DisplayName("상품이 예약 상태이면 예약 일시와 장소 정보를 함께 반환한다")
        void it_returns_reservation_schedule_and_place_when_reserved() {
            arrangeRoomAsSellerView();
            arrangeEmptyMessages();
            givenTradeState(false, true, null);

            ProductReservation reservation = mock(ProductReservation.class);
            LocalDateTime scheduledAt = LocalDateTime.of(2026, 9, 1, 14, 0);
            when(reservation.getScheduledAt()).thenReturn(scheduledAt);
            when(reservation.getPlaceName()).thenReturn("광산구청");
            when(reservation.getAddress()).thenReturn("광주광역시 광산구 광산로29번길 15");
            when(reservation.getLatitude()).thenReturn(35.1397);
            when(reservation.getLongitude()).thenReturn(126.7935);
            when(productReservationRepository.findByProductAndStatus(product, ReservationStatus.PENDING))
                    .thenReturn(Optional.of(reservation));

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.product().reservationScheduledAt()).isEqualTo(scheduledAt);
            assertThat(response.product().reservationPlaceName()).isEqualTo("광산구청");
            assertThat(response.product().reservationAddress()).isEqualTo("광주광역시 광산구 광산로29번길 15");
            assertThat(response.product().reservationLatitude()).isEqualTo(35.1397);
            assertThat(response.product().reservationLongitude()).isEqualTo(126.7935);
        }

        @Test
        @DisplayName("상품이 예약 상태가 아니면 예약 조회 없이 예약 정보는 null 이다")
        void it_does_not_query_reservation_when_not_reserved() {
            arrangeRoomAsSellerView();
            arrangeEmptyMessages();

            GetChatMessagesResponse response = service.execute(5L, null, null, 20);

            assertThat(response.product().reservationScheduledAt()).isNull();
            assertThat(response.product().reservationPlaceName()).isNull();
            assertThat(response.product().reservationAddress()).isNull();
            assertThat(response.product().reservationLatitude()).isNull();
            assertThat(response.product().reservationLongitude()).isNull();
            verifyNoInteractions(productReservationRepository);
        }

        @Test
        @DisplayName("메시지가 있으면 가장 최근 메시지 id 까지 읽음 처리한다")
        void it_marks_messages_as_read_using_latest_message_id_when_messages_exist() {
            arrangeRoomAsSellerView();
            ChatMessage msg = buildTextMessage(7L, "안녕");
            when(chatMessageRepository.findChatMessageByRoomIdWithCursorPaging(eq(5L), any(), any(), anyInt()))
                    .thenReturn(List.of(msg));

            service.execute(5L, null, null, 20);

            verify(chatMessageRepository).readMessage(5L, 7L, 1L);
        }

        @Test
        @DisplayName("메시지가 없으면 읽음 처리를 호출하지 않는다")
        void it_does_not_mark_as_read_when_no_messages() {
            arrangeRoomAsSellerView();
            arrangeEmptyMessages();

            service.execute(5L, null, null, 20);

            verify(chatMessageRepository, never()).readMessage(anyLong(), anyLong(), anyLong());
        }

        private ChatMessage buildTextMessage(Long id, String content) {
            ChatMessage msg = mock(ChatMessage.class);
            ChatRoom msgRoom = mock(ChatRoom.class);
            when(msg.getId()).thenReturn(id);
            when(msg.getRoom()).thenReturn(msgRoom);
            when(msgRoom.getId()).thenReturn(5L);
            when(msg.getContent()).thenReturn(content);
            when(msg.getMessageType()).thenReturn(MessageType.TEXT);
            when(msg.getCreatedAt()).thenReturn(LocalDateTime.now());
            when(msg.getSender()).thenReturn(currentMember);
            when(msg.getChecked()).thenReturn(false);
            when(currentMember.getNickname()).thenReturn("닉네임");
            return msg;
        }

        private ChatMessage buildImageMessage(Long id, String content) {
            ChatMessage msg = mock(ChatMessage.class);
            ChatRoom msgRoom = mock(ChatRoom.class);
            when(msg.getId()).thenReturn(id);
            when(msg.getRoom()).thenReturn(msgRoom);
            when(msgRoom.getId()).thenReturn(5L);
            when(msg.getContent()).thenReturn(content);
            when(msg.getMessageType()).thenReturn(MessageType.IMAGE);
            when(msg.getCreatedAt()).thenReturn(LocalDateTime.now());
            when(msg.getSender()).thenReturn(currentMember);
            when(msg.getChecked()).thenReturn(false);
            when(currentMember.getNickname()).thenReturn("닉네임");
            return msg;
        }
    }
}
