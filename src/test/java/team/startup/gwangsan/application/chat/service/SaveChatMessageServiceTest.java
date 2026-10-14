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
import team.startup.gwangsan.application.chat.data.SaveChatMessageCommand;
import team.startup.gwangsan.application.chat.data.SavedChatMessage;
import team.startup.gwangsan.application.chat.port.out.ChatBlockPort;
import team.startup.gwangsan.application.chat.port.out.ChatMemberPort;
import team.startup.gwangsan.application.chat.port.out.ChatMessagePort;
import team.startup.gwangsan.application.chat.port.out.ChatPushPort;
import team.startup.gwangsan.application.chat.port.out.ChatRoomPort;
import team.startup.gwangsan.domain.chat.exception.ChatMessageIdConflictException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatMessageException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatSenderException;
import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.chat.model.ChatRoom;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.chat.model.StoredChatMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaveChatMessageService 단위 테스트")
class SaveChatMessageServiceTest {

    private static final Long BUYER = 1L;
    private static final Long SELLER = 2L;
    private static final Long ROOM = 10L;
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final ChatImage IMAGE = new ChatImage(5L, "https://example.test/5");

    @Mock private ChatMessagePort messagePort;
    @Mock private ChatRoomPort roomPort;
    @Mock private ChatMemberPort memberPort;
    @Mock private ChatBlockPort blockPort;
    @Mock private ChatPushPort pushPort;
    @InjectMocks private SaveChatMessageService service;

    private static SaveChatMessageCommand command(Long sender, MessageType type, List<Long> imageIds) {
        return new SaveChatMessageCommand(100L, ROOM, sender, type == MessageType.IMAGE ? null : "hello", imageIds, type, CREATED);
    }

    private void givenNewMessage(ChatRoom room) {
        when(messagePort.exists(100L)).thenReturn(false);
        when(memberPort.exists(anyLong())).thenReturn(true);
        when(roomPort.load(ROOM)).thenReturn(Optional.of(room));
        when(messagePort.insertIfAbsent(any())).thenReturn(true);
    }

    private static ChatRoom room() {
        return new ChatRoom(ROOM, 20L, BUYER, SELLER, null, null);
    }

    @Nested
    @DisplayName("새 메시지는")
    class Describe_new_message {

        @Test
        @DisplayName("it_발신자가 없으면 저장 없이 NotFoundChatSenderException 을 던진다")
        void it_rejects_missing_sender() {
            when(memberPort.exists(BUYER)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(command(BUYER, MessageType.TEXT, null)))
                    .isInstanceOf(NotFoundChatSenderException.class);
            verify(messagePort, never()).insertIfAbsent(any());
            verifyNoInteractions(roomPort, blockPort, pushPort);
        }

        @Test
        @DisplayName("it_채팅방이 없으면 저장 없이 NotFoundChatRoomException 을 던진다")
        void it_rejects_missing_room() {
            when(memberPort.exists(BUYER)).thenReturn(true);
            when(roomPort.load(ROOM)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command(BUYER, MessageType.TEXT, null)))
                    .isInstanceOf(NotFoundChatRoomException.class);
            verify(messagePort, never()).insertIfAbsent(any());
        }

        @Test
        @DisplayName("it_TEXT 를 저장하고 상대에게 푸시하며 숨김 상태는 바꾸지 않는다")
        void it_saves_text_and_notifies_recipient() {
            givenNewMessage(room());

            SavedChatMessage saved = service.execute(command(BUYER, MessageType.TEXT, null));

            assertThat(saved).isEqualTo(new SavedChatMessage(100L, List.of(), CREATED, BUYER, false));
            verify(pushPort).notifyNewMessage(SELLER, ROOM);
            verify(messagePort, never()).linkImages(anyLong(), any());
            verify(roomPort, never()).saveHiddenState(any());
        }

        @Test
        @DisplayName("it_INSERT 선점 뒤에 차단을 검사한다 (차단이면 rollback 대상 예외, 이미지·푸시 없음)")
        void it_checks_block_after_insert() {
            givenNewMessage(room());
            RuntimeException blocked = new RuntimeException("blocked");
            doThrow(blocked).when(blockPort).validateNotBlocked(BUYER, SELLER);

            assertThatThrownBy(() -> service.execute(command(BUYER, MessageType.IMAGE, List.of(5L)))).isSameAs(blocked);

            InOrder order = inOrder(messagePort, blockPort);
            order.verify(messagePort).insertIfAbsent(any());
            order.verify(blockPort).validateNotBlocked(BUYER, SELLER);
            verify(messagePort, never()).linkImages(anyLong(), any());
            verifyNoInteractions(pushPort);
        }

        @Test
        @DisplayName("it_IMAGE 이고 imageIds 가 있으면 링크 결과를 응답한다")
        void it_links_images() {
            givenNewMessage(room());
            when(messagePort.linkImages(100L, List.of(5L, 999L))).thenReturn(List.of(IMAGE));

            SavedChatMessage saved = service.execute(command(BUYER, MessageType.IMAGE, List.of(5L, 999L)));

            assertThat(saved.images()).containsExactly(IMAGE);
        }

        @ParameterizedTest
        @CsvSource(value = {"null", "empty"}, nullValues = "null")
        @DisplayName("it_IMAGE 라도 imageIds 가 없거나 비면 링크하지 않는다")
        void it_skips_missing_image_ids(String kind) {
            givenNewMessage(room());

            service.execute(command(BUYER, MessageType.IMAGE, kind == null ? null : List.of()));

            verify(messagePort, never()).linkImages(anyLong(), any());
        }

        @Test
        @DisplayName("it_SYSTEM 메시지는 푸시하지 않는다")
        void it_does_not_push_system_message() {
            givenNewMessage(room());

            service.execute(new SaveChatMessageCommand(100L, ROOM, BUYER, "시스템", null, MessageType.SYSTEM, CREATED));

            verifyNoInteractions(pushPort);
        }

        @ParameterizedTest
        @CsvSource({"true, true", "true, false", "false, true", "false, false"})
        @DisplayName("it_수신자가 방을 숨겼으면 푸시하지 않는다 (양방향)")
        void it_respects_recipient_hidden_state(boolean senderIsBuyer, boolean recipientHidden) {
            Long sender = senderIsBuyer ? BUYER : SELLER;
            Long recipient = senderIsBuyer ? SELLER : BUYER;
            ChatRoom room = recipientHidden ? room().hideFor(recipient, CREATED) : room();
            givenNewMessage(room);

            service.execute(command(sender, MessageType.TEXT, null));

            if (recipientHidden) {
                verifyNoInteractions(pushPort);
            } else {
                verify(pushPort).notifyNewMessage(recipient, ROOM);
            }
        }
    }

    @Nested
    @DisplayName("재수신 메시지는")
    class Describe_replay {

        private StoredChatMessage stored(MessageType type, List<ChatImage> images) {
            return new StoredChatMessage(100L, ROOM, BUYER, type == MessageType.IMAGE ? null : "hello", type, CREATED,
                    true, false, images);
        }

        @Test
        @DisplayName("it_이미 있으면 저장 결과를 돌려주고 INSERT·차단·푸시를 반복하지 않는다")
        void it_replays_existing_message() {
            when(messagePort.exists(100L)).thenReturn(true);
            when(messagePort.findStored(100L)).thenReturn(Optional.of(stored(MessageType.TEXT, List.of())));

            SavedChatMessage saved = service.execute(command(BUYER, MessageType.TEXT, null));

            assertThat(saved).isEqualTo(new SavedChatMessage(100L, List.of(), CREATED, BUYER, true));
            verify(messagePort, never()).insertIfAbsent(any());
            verifyNoInteractions(memberPort, roomPort, blockPort, pushPort);
        }

        @Test
        @DisplayName("it_INSERT 경쟁에서 지면 replay 하고 후속 부작용을 실행하지 않는다")
        void it_replays_after_insert_race() {
            when(messagePort.exists(100L)).thenReturn(false);
            when(memberPort.exists(BUYER)).thenReturn(true);
            when(roomPort.load(ROOM)).thenReturn(Optional.of(room()));
            when(messagePort.insertIfAbsent(any())).thenReturn(false);
            when(messagePort.findStored(100L)).thenReturn(Optional.of(stored(MessageType.TEXT, List.of())));

            service.execute(command(BUYER, MessageType.TEXT, null));

            verifyNoInteractions(blockPort, pushPort);
        }

        @Test
        @DisplayName("it_있다고 본 메시지를 읽을 수 없으면 NotFoundChatMessageException 이다")
        void it_fails_when_stored_message_disappears() {
            when(messagePort.exists(100L)).thenReturn(true);
            when(messagePort.findStored(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command(BUYER, MessageType.TEXT, null)))
                    .isInstanceOf(NotFoundChatMessageException.class);
        }

        @Test
        @DisplayName("it_내용이 다르면 ChatMessageIdConflictException 이다")
        void it_rejects_conflicting_payload() {
            when(messagePort.exists(100L)).thenReturn(true);
            when(messagePort.findStored(100L)).thenReturn(Optional.of(stored(MessageType.TEXT, List.of())));

            assertThatThrownBy(() -> service.execute(new SaveChatMessageCommand(100L, ROOM, BUYER, "다름", null,
                    MessageType.TEXT, CREATED))).isInstanceOf(ChatMessageIdConflictException.class);
        }

        @Test
        @DisplayName("it_IMAGE 는 null imageIds 와 빈 저장 이미지를 같게 보고 존재 이미지 조회를 생략한다")
        void it_treats_null_image_ids_as_empty() {
            when(messagePort.exists(100L)).thenReturn(true);
            when(messagePort.findStored(100L)).thenReturn(Optional.of(stored(MessageType.IMAGE, List.of())));

            service.execute(command(BUYER, MessageType.IMAGE, null));

            verify(messagePort, never()).findExistingImageIds(any());
        }

        @Test
        @DisplayName("it_IMAGE 는 존재하는 요청 이미지 집합으로 비교한다")
        void it_compares_existing_image_ids() {
            when(messagePort.exists(100L)).thenReturn(true);
            when(messagePort.findStored(100L)).thenReturn(Optional.of(stored(MessageType.IMAGE, List.of(IMAGE))));
            when(messagePort.findExistingImageIds(List.of(5L, 5L, 999L))).thenReturn(Set.of(5L));
            when(messagePort.findExistingImageIds(List.of(6L))).thenReturn(Set.of(6L));

            assertThat(service.execute(command(BUYER, MessageType.IMAGE, List.of(5L, 5L, 999L))).images())
                    .containsExactly(IMAGE);
            assertThatThrownBy(() -> service.execute(command(BUYER, MessageType.IMAGE, List.of(6L))))
                    .isInstanceOf(ChatMessageIdConflictException.class);
        }
    }
}
