package team.startup.gwangsan.application.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.chat.data.ChatMessageRow;
import team.startup.gwangsan.application.chat.data.ChatRoomMessages;
import team.startup.gwangsan.application.chat.data.ChatTradeCard;
import team.startup.gwangsan.application.chat.data.LoadChatRoomMessagesCommand;
import team.startup.gwangsan.application.chat.port.out.ChatMessageQueryPort;
import team.startup.gwangsan.application.chat.port.out.ChatProductPort;
import team.startup.gwangsan.application.chat.port.out.ChatRoomPort;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.chat.model.ChatRoom;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.common.ActorContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoadChatRoomMessagesService 단위 테스트")
class LoadChatRoomMessagesServiceTest {

    private static final Long ME = 1L;
    private static final Long OTHER = 2L;
    private static final Long ROOM = 5L;
    private static final Long PRODUCT = 10L;
    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 8, 30, 11, 20);
    private static final LocalDateTime CURSOR = LocalDateTime.of(2026, 1, 2, 3, 4, 5);

    @Mock private ChatRoomPort roomPort;
    @Mock private ChatProductPort productPort;
    @Mock private ChatMessageQueryPort messageQueryPort;
    @InjectMocks private LoadChatRoomMessagesService service;

    private static LoadChatRoomMessagesCommand command() {
        return new LoadChatRoomMessagesCommand(new ActorContext(ME, ActorContext.Role.ROLE_USER), ROOM, CURSOR, 9L, 3);
    }

    private static ChatTradeCard card(Long ownerId, boolean bySeller, boolean byBuyer, ChatTradeCard.Reservation reservation) {
        return new ChatTradeCard(PRODUCT, "상품명", List.of(new ChatImage(1L, "p")), ownerId, false, false,
                reservation != null, bySeller, byBuyer, REQUESTED_AT, reservation, null);
    }

    private void givenRoom(boolean meIsSeller, ChatTradeCard card) {
        ChatRoom room = meIsSeller
                ? new ChatRoom(ROOM, PRODUCT, OTHER, ME, null, null)
                : new ChatRoom(ROOM, PRODUCT, ME, OTHER, null, null);
        when(roomPort.loadWithProduct(ROOM)).thenReturn(Optional.of(room));
        when(productPort.loadTradeCard(PRODUCT, room.buyerId(), room.sellerId())).thenReturn(card);
    }

    private static ChatMessageRow row(Long id, MessageType type, Long sender) {
        return new ChatMessageRow(id, ROOM, "내용" + id, type, CURSOR.minusMinutes(id), null, sender, "닉" + sender, false);
    }

    @Nested
    @DisplayName("접근 제어는")
    class Describe_access {

        @Test
        @DisplayName("it_방이 없으면 NotFoundChatRoomException 이다")
        void it_rejects_missing_room() {
            when(roomPort.loadWithProduct(ROOM)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command())).isInstanceOf(NotFoundChatRoomException.class);
        }

        @Test
        @DisplayName("it_참여자가 아니면 상품·메시지를 읽지 않고 NotFoundChatRoomException 이다")
        void it_rejects_non_participant() {
            when(roomPort.loadWithProduct(ROOM)).thenReturn(Optional.of(new ChatRoom(ROOM, PRODUCT, 7L, 8L, null, null)));

            assertThatThrownBy(() -> service.execute(command())).isInstanceOf(NotFoundChatRoomException.class);
            verifyNoInteractions(productPort, messageQueryPort);
        }
    }

    @Nested
    @DisplayName("상품 카드는")
    class Describe_card {

        @ParameterizedTest
        @CsvSource({"true, true", "true, false", "false, true", "false, false"})
        @DisplayName("it_보는 사람 역할로 isSeller·isCompletable 을, 작성자 ID 로 isAuthor 를 정한다")
        void it_derives_viewer_flags(boolean meIsSeller, boolean meIsAuthor) {
            givenRoom(meIsSeller, card(meIsAuthor ? ME : OTHER, true, false, null));
            when(messageQueryPort.findPage(ROOM, CURSOR, 9L, 3)).thenReturn(List.of());

            ChatRoomMessages result = service.execute(command());

            assertThat(result.product().isSeller()).isEqualTo(meIsSeller);
            assertThat(result.product().isAuthor()).isEqualTo(meIsAuthor);
            assertThat(result.product().isCompletable()).isEqualTo(meIsSeller);
            assertThat(result.product().createdAt()).isEqualTo(REQUESTED_AT);
            assertThat(result.product().images()).containsExactly(new ChatImage(1L, "p"));
        }

        @Test
        @DisplayName("it_예약 정보를 그대로, 예약이 없으면 null 로 내려준다")
        void it_maps_reservation() {
            var reservation = new ChatTradeCard.Reservation(CURSOR, REQUESTED_AT, "광산구청", "주소", 35.1, 126.7);
            givenRoom(true, card(ME, true, true, reservation));
            when(messageQueryPort.findPage(ROOM, CURSOR, 9L, 3)).thenReturn(List.of());

            var product = service.execute(command()).product();

            assertThat(product.isReserved()).isTrue();
            assertThat(product.reservationScheduledAt()).isEqualTo(CURSOR);
            assertThat(product.reservedAt()).isEqualTo(REQUESTED_AT);
            assertThat(product.reservationPlaceName()).isEqualTo("광산구청");
            assertThat(product.reservationAddress()).isEqualTo("주소");
            assertThat(product.reservationLatitude()).isEqualTo(35.1);
            assertThat(product.reservationLongitude()).isEqualTo(126.7);
        }

        @Test
        @DisplayName("it_예약이 없으면 예약 필드는 모두 null 이다")
        void it_maps_absent_reservation() {
            givenRoom(true, card(ME, true, true, null));
            when(messageQueryPort.findPage(ROOM, CURSOR, 9L, 3)).thenReturn(List.of());

            var product = service.execute(command()).product();

            assertThat(product.reservationScheduledAt()).isNull();
            assertThat(product.reservedAt()).isNull();
            assertThat(product.reservationPlaceName()).isNull();
            assertThat(product.reservationLatitude()).isNull();
        }
    }

    @Nested
    @DisplayName("메시지와 읽음 처리는")
    class Describe_messages {

        @Test
        @DisplayName("it_IMAGE 메시지만 이미지 조회 후 매핑하고 isMine 을 계산한다")
        void it_maps_messages_and_images() {
            givenRoom(true, card(ME, true, true, null));
            when(messageQueryPort.findPage(ROOM, CURSOR, 9L, 3))
                    .thenReturn(List.of(row(3L, MessageType.IMAGE, OTHER), row(2L, MessageType.TEXT, ME)));
            when(messageQueryPort.findImages(List.of(3L))).thenReturn(Map.of(3L, List.of(new ChatImage(8L, "u"))));

            var messages = service.execute(command()).messages();

            assertThat(messages).extracting("messageId").containsExactly(3L, 2L);
            assertThat(messages.get(0).images()).containsExactly(new ChatImage(8L, "u"));
            assertThat(messages.get(0).isMine()).isFalse();
            assertThat(messages.get(1).images()).isEmpty();
            assertThat(messages.get(1).isMine()).isTrue();
            assertThat(messages.get(1).senderNickname()).isEqualTo("닉1");
        }

        @Test
        @DisplayName("it_TEXT 뿐이면 이미지를 조회하지 않는다")
        void it_skips_image_lookup_for_text_only() {
            givenRoom(true, card(ME, true, true, null));
            when(messageQueryPort.findPage(ROOM, CURSOR, 9L, 3)).thenReturn(List.of(row(2L, MessageType.TEXT, ME)));

            service.execute(command());

            verify(messageQueryPort, never()).findImages(any());
        }

        @Test
        @DisplayName("it_응답을 만든 뒤 페이지의 최신 메시지 ID 까지 읽음 처리한다")
        void it_marks_read_after_loading() {
            givenRoom(true, card(ME, true, true, null));
            when(messageQueryPort.findPage(ROOM, CURSOR, 9L, 3))
                    .thenReturn(List.of(row(7L, MessageType.TEXT, OTHER), row(6L, MessageType.TEXT, OTHER)));

            service.execute(command());

            InOrder order = inOrder(productPort, messageQueryPort);
            order.verify(productPort).loadTradeCard(anyLong(), anyLong(), anyLong());
            order.verify(messageQueryPort).findPage(ROOM, CURSOR, 9L, 3);
            order.verify(messageQueryPort).markRead(ROOM, 7L, ME);
        }

        @Test
        @DisplayName("it_메시지가 없으면 읽음 처리하지 않는다")
        void it_does_not_mark_read_without_messages() {
            givenRoom(false, card(OTHER, true, true, null));
            when(messageQueryPort.findPage(ROOM, CURSOR, 9L, 3)).thenReturn(List.of());

            service.execute(command());

            verify(messageQueryPort, never()).markRead(anyLong(), anyLong(), anyLong());
        }
    }
}
