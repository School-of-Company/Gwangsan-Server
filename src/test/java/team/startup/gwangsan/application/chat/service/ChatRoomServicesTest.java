package team.startup.gwangsan.application.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.chat.data.ChatProduct;
import team.startup.gwangsan.application.chat.data.ChatRoomListRow;
import team.startup.gwangsan.application.chat.data.ChatRoomProduct;
import team.startup.gwangsan.application.chat.data.ChatRoomSnapshot;
import team.startup.gwangsan.application.chat.port.out.ChatBlockPort;
import team.startup.gwangsan.application.chat.port.out.ChatMessagePort;
import team.startup.gwangsan.application.chat.port.out.ChatMessageQueryPort;
import team.startup.gwangsan.application.chat.port.out.ChatProductPort;
import team.startup.gwangsan.application.chat.port.out.ChatRoomListPort;
import team.startup.gwangsan.application.chat.port.out.ChatRoomPort;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.model.ChatRoom;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.common.ActorContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** 방 생성·숨김·목록·ID 조회·읽음·공개 조회 유스케이스. */
@ExtendWith(MockitoExtension.class)
@DisplayName("채팅방 유스케이스 단위 테스트")
class ChatRoomServicesTest {

    private static final Long ME = 1L;
    private static final Long OWNER = 2L;
    private static final Long PRODUCT = 10L;
    private static final ActorContext ACTOR = new ActorContext(ME, ActorContext.Role.ROLE_USER);

    @Mock private ChatRoomPort roomPort;
    @Mock private ChatRoomListPort roomListPort;
    @Mock private ChatProductPort productPort;
    @Mock private ChatBlockPort blockPort;
    @Mock private ChatMessagePort messagePort;
    @Mock private ChatMessageQueryPort messageQueryPort;

    @Nested
    @DisplayName("CreateChatRoomService 는")
    class Describe_create {

        private CreateChatRoomService service() {
            return new CreateChatRoomService(productPort, blockPort, roomPort);
        }

        @Test
        @DisplayName("it_GIVER 상품이면 요청자가 구매자, 작성자가 판매자인 방을 만든다")
        void it_creates_giver_room() {
            when(productPort.loadActive(PRODUCT)).thenReturn(new ChatProduct(PRODUCT, OWNER, true));
            when(roomPort.findByProductAndParticipants(PRODUCT, ME, OWNER)).thenReturn(Optional.empty());
            when(roomPort.create(PRODUCT, ME, OWNER)).thenReturn(41L);

            assertThat(service().execute(ACTOR, PRODUCT)).isEqualTo(41L);
        }

        @Test
        @DisplayName("it_RECEIVER 상품이면 작성자가 구매자, 요청자가 판매자다")
        void it_creates_receiver_room() {
            when(productPort.loadActive(PRODUCT)).thenReturn(new ChatProduct(PRODUCT, OWNER, false));
            when(roomPort.findByProductAndParticipants(PRODUCT, OWNER, ME)).thenReturn(Optional.empty());
            when(roomPort.create(PRODUCT, OWNER, ME)).thenReturn(42L);

            assertThat(service().execute(ACTOR, PRODUCT)).isEqualTo(42L);
        }

        @Test
        @DisplayName("it_자기 상품이면 구매자와 판매자가 같은 방을 만든다 (기존 동작)")
        void it_creates_self_room() {
            when(productPort.loadActive(PRODUCT)).thenReturn(new ChatProduct(PRODUCT, ME, true));
            when(roomPort.findByProductAndParticipants(PRODUCT, ME, ME)).thenReturn(Optional.empty());
            when(roomPort.create(PRODUCT, ME, ME)).thenReturn(43L);

            assertThat(service().execute(ACTOR, PRODUCT)).isEqualTo(43L);
        }

        @Test
        @DisplayName("it_기존 방이 있으면 요청자 숨김만 풀고 새로 만들지 않는다")
        void it_reuses_existing_room() {
            ChatRoom hidden = new ChatRoom(40L, PRODUCT, ME, OWNER, LocalDateTime.now(), LocalDateTime.now());
            when(productPort.loadActive(PRODUCT)).thenReturn(new ChatProduct(PRODUCT, OWNER, true));
            when(roomPort.findByProductAndParticipants(PRODUCT, ME, OWNER)).thenReturn(Optional.of(hidden));

            assertThat(service().execute(ACTOR, PRODUCT)).isEqualTo(40L);

            ArgumentCaptor<ChatRoom> saved = ArgumentCaptor.forClass(ChatRoom.class);
            verify(roomPort).saveHiddenState(saved.capture());
            assertThat(saved.getValue().hiddenByBuyerAt()).isNull();
            assertThat(saved.getValue().hiddenBySellerAt()).isEqualTo(hidden.hiddenBySellerAt());
            verify(roomPort, never()).create(anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("it_상품 부재·차단 오류를 그대로 전파하고 방을 찾지 않는다")
        void it_propagates_product_and_block_failures() {
            RuntimeException missing = new RuntimeException("no product");
            when(productPort.loadActive(PRODUCT)).thenThrow(missing);
            assertThatThrownBy(() -> service().execute(ACTOR, PRODUCT)).isSameAs(missing);

            reset(productPort);
            RuntimeException blocked = new RuntimeException("blocked");
            when(productPort.loadActive(PRODUCT)).thenReturn(new ChatProduct(PRODUCT, OWNER, true));
            doThrow(blocked).when(blockPort).validateNotBlocked(ME, OWNER);
            assertThatThrownBy(() -> service().execute(ACTOR, PRODUCT)).isSameAs(blocked);
            verifyNoInteractions(roomPort);
        }
    }

    @Nested
    @DisplayName("HideChatRoomService 는")
    class Describe_hide {

        @Test
        @DisplayName("it_방이 없거나 비참여자면 404 이고 숨기지 않는다")
        void it_rejects_missing_or_stranger() {
            HideChatRoomService service = new HideChatRoomService(roomPort);
            when(roomPort.load(2L)).thenReturn(Optional.empty());
            when(roomPort.load(3L)).thenReturn(Optional.of(new ChatRoom(3L, PRODUCT, 7L, 8L, null, null)));

            assertThatThrownBy(() -> service.execute(ACTOR, 2L)).isInstanceOf(NotFoundChatRoomException.class);
            assertThatThrownBy(() -> service.execute(ACTOR, 3L)).isInstanceOf(NotFoundChatRoomException.class);
            verify(roomPort, never()).saveHiddenState(any());
        }

        @Test
        @DisplayName("it_요청자 쪽만 숨김 시각을 기록한다")
        void it_hides_only_for_requester() {
            when(roomPort.load(3L)).thenReturn(Optional.of(new ChatRoom(3L, PRODUCT, OWNER, ME, null, null)));

            new HideChatRoomService(roomPort).execute(ACTOR, 3L);

            ArgumentCaptor<ChatRoom> saved = ArgumentCaptor.forClass(ChatRoom.class);
            verify(roomPort).saveHiddenState(saved.capture());
            assertThat(saved.getValue().hiddenBySellerAt()).isNotNull();
            assertThat(saved.getValue().hiddenByBuyerAt()).isNull();
        }
    }

    @Nested
    @DisplayName("ChatRoomListService 는")
    class Describe_list {

        private ChatRoomListRow row(Long roomId, Long productId) {
            return new ChatRoomListRow(roomId, OWNER, "상대", 9L, "안녕", MessageType.TEXT, null, 1L, productId);
        }

        @Test
        @DisplayName("it_방마다 상품을 붙이고 조회되지 않은 상품은 삭제 placeholder 로 채운다")
        void it_attaches_products() {
            ChatRoomProduct product = new ChatRoomProduct(10L, "상품", false, true, false, List.of());
            when(roomListPort.findVisibleRooms(ME)).thenReturn(List.of(row(1L, 10L), row(2L, 11L)));
            when(productPort.findRoomProducts(Set.of(10L, 11L))).thenReturn(Map.of(10L, product));

            var result = new ChatRoomListService(roomListPort, productPort).query(ACTOR);

            assertThat(result).extracting(summary -> summary.room().roomId()).containsExactly(1L, 2L);
            assertThat(result.get(0).product()).isEqualTo(product);
            assertThat(result.get(1).product()).isEqualTo(new ChatRoomProduct(11L, null, false, false, true, List.of()));
        }

        @Test
        @DisplayName("it_방이 없어도 상품 조회를 빈 집합으로 호출하고 빈 목록을 돌려준다 (기존 호출 유지)")
        void it_returns_empty() {
            when(roomListPort.findVisibleRooms(ME)).thenReturn(List.of());
            when(productPort.findRoomProducts(Set.of())).thenReturn(Map.of());

            assertThat(new ChatRoomListService(roomListPort, productPort).query(ACTOR)).isEmpty();
        }
    }

    @Nested
    @DisplayName("ChatRoomIdService·ReadChatMessagesService 는")
    class Describe_simple {

        @Test
        @DisplayName("it_상품·회원으로 방 ID 를 찾고 없으면 404 다")
        void it_finds_room_id() {
            ChatRoomIdService service = new ChatRoomIdService(roomPort);
            when(roomPort.findByProductAndMember(PRODUCT, ME)).thenReturn(Optional.of(new ChatRoom(2L, PRODUCT, ME, OWNER, null, null)));
            when(roomPort.findByProductAndMember(99L, ME)).thenReturn(Optional.empty());

            assertThat(service.query(ACTOR, PRODUCT)).isEqualTo(2L);
            assertThatThrownBy(() -> service.query(ACTOR, 99L)).isInstanceOf(NotFoundChatRoomException.class);
        }

        @Test
        @DisplayName("it_인증 실행자 ID 로 읽음 처리한다")
        void it_marks_read_with_actor() {
            new ReadChatMessagesService(messageQueryPort).execute(ACTOR, 12L, 33L);

            verify(messageQueryPort).markRead(12L, 33L, ME);
        }
    }

    @Nested
    @DisplayName("ChatRoomLookupService 는")
    class Describe_lookup {

        @Test
        @DisplayName("it_다른 업무에 방 사실만 공개하고 대화 여부를 위임한다")
        void it_exposes_snapshots() {
            ChatRoomLookupService service = new ChatRoomLookupService(roomPort, messagePort);
            ChatRoom room = new ChatRoom(2L, PRODUCT, ME, OWNER, LocalDateTime.now(), null);
            when(roomPort.load(2L)).thenReturn(Optional.of(room));
            when(roomPort.findByProductAndParticipants(PRODUCT, ME, OWNER)).thenReturn(Optional.of(room));
            when(roomPort.findByProductAndMember(PRODUCT, OWNER)).thenReturn(Optional.empty());
            when(messagePort.hasSentMessage(2L, ME)).thenReturn(true);

            ChatRoomSnapshot expected = new ChatRoomSnapshot(2L, PRODUCT, ME, OWNER);
            assertThat(service.byRoomId(2L)).contains(expected);
            assertThat(service.byProductAndParticipants(PRODUCT, ME, OWNER)).contains(expected);
            assertThat(service.byProductAndMember(PRODUCT, OWNER)).isEmpty();
            assertThat(service.hasSentMessage(2L, ME)).isTrue();
        }
    }

    @Test
    @DisplayName("방 생성은 상품 확인 → 차단 → 기존 방 조회 순서다")
    void it_checks_create_in_order() {
        when(productPort.loadActive(PRODUCT)).thenReturn(new ChatProduct(PRODUCT, OWNER, true));
        when(roomPort.findByProductAndParticipants(PRODUCT, ME, OWNER)).thenReturn(Optional.empty());

        new CreateChatRoomService(productPort, blockPort, roomPort).execute(ACTOR, PRODUCT);

        InOrder order = inOrder(productPort, blockPort, roomPort);
        order.verify(productPort).loadActive(PRODUCT);
        order.verify(blockPort).validateNotBlocked(ME, OWNER);
        order.verify(roomPort).findByProductAndParticipants(PRODUCT, ME, OWNER);
    }
}
