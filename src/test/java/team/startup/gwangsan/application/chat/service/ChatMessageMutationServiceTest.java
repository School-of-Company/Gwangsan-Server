package team.startup.gwangsan.application.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.chat.data.ChatMessageMutationEvent;
import team.startup.gwangsan.application.chat.data.UpdatedChatMessage;
import team.startup.gwangsan.application.chat.port.out.ChatMessageMutationPort;
import team.startup.gwangsan.application.chat.port.out.ChatMessagePort;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatMessageException;
import team.startup.gwangsan.domain.chat.model.ChatMessage;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageMutationService 단위 테스트")
class ChatMessageMutationServiceTest {

    private static final ActorContext ACTOR = new ActorContext(1L, ActorContext.Role.ROLE_USER);
    private static final Long ROOM = 3L;

    @Mock private ChatMessagePort messagePort;
    @Mock private ChatMessageMutationPort mutationPort;
    @InjectMocks private ChatMessageMutationService service;

    private static ChatMessage message(Long id, MessageType type, LocalDateTime deletedAt) {
        return new ChatMessage(id, ROOM, 1L, "원문", null, type, LocalDateTime.now().minusMinutes(id), null, deletedAt);
    }

    private ChatMessageMutationEvent publishedEvent() {
        ArgumentCaptor<ChatMessageMutationEvent> event = ArgumentCaptor.forClass(ChatMessageMutationEvent.class);
        verify(mutationPort).publish(event.capture());
        return event.getValue();
    }

    @Nested
    @DisplayName("수정은")
    class Describe_update {

        @Test
        @DisplayName("it_없거나 삭제된 메시지는 404 이고 아무것도 쓰지 않는다")
        void it_rejects_missing_or_deleted() {
            when(messagePort.findForUpdate(7L)).thenReturn(Optional.empty());
            when(messagePort.findForUpdate(8L)).thenReturn(Optional.of(message(8L, MessageType.TEXT, LocalDateTime.now())));

            assertThatThrownBy(() -> service.execute(ACTOR, 7L, "수정")).isInstanceOf(NotFoundChatMessageException.class);
            assertThatThrownBy(() -> service.execute(ACTOR, 8L, "수정")).isInstanceOf(NotFoundChatMessageException.class);
            verify(messagePort, never()).saveEdit(any());
            verifyNoInteractions(mutationPort);
        }

        @Test
        @DisplayName("it_텍스트·내용 검증을 최신 여부 조회보다 먼저 한다")
        void it_validates_before_latest_lookup() {
            when(messagePort.findForUpdate(7L)).thenReturn(Optional.of(message(7L, MessageType.IMAGE, null)));

            assertThatThrownBy(() -> service.execute(ACTOR, 7L, "수정"))
                    .isInstanceOf(GlobalException.class).extracting("errorCode").isEqualTo(ErrorCode.CHAT_MESSAGE_NOT_TEXT);
            verify(messagePort, never()).findLatest(any());
        }

        @Test
        @DisplayName("it_최신 메시지를 고치면 원문을 보존해 저장하고 새 최신 메시지를 함께 알린다")
        void it_updates_latest_message() {
            ChatMessage original = message(7L, MessageType.TEXT, null);
            when(messagePort.findForUpdate(7L)).thenReturn(Optional.of(original));
            when(messagePort.findLatest(ROOM)).thenReturn(Optional.of(original));

            UpdatedChatMessage updated = service.execute(ACTOR, 7L, "수정");

            ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
            verify(messagePort).saveEdit(saved.capture());
            assertThat(saved.getValue().content()).isEqualTo("수정");
            assertThat(saved.getValue().originalContent()).isEqualTo("원문");
            assertThat(updated.editedAt()).isEqualTo(saved.getValue().editedAt()).isNotNull();
            ChatMessageMutationEvent event = publishedEvent();
            assertThat(event.kind()).isEqualTo(ChatMessageMutationEvent.Kind.UPDATED);
            assertThat(event.roomListChanged()).isTrue();
            assertThat(event.latestMessage().content()).isEqualTo("수정");
            assertThat(event.latestMessage().editedAt()).isEqualTo(updated.editedAt());
        }

        @Test
        @DisplayName("it_최신이 아니면 latestMessage 없이 알린다")
        void it_updates_older_message() {
            when(messagePort.findForUpdate(7L)).thenReturn(Optional.of(message(7L, MessageType.TEXT, null)));
            when(messagePort.findLatest(ROOM)).thenReturn(Optional.of(message(9L, MessageType.SYSTEM, null)));

            service.execute(ACTOR, 7L, "수정");

            assertThat(publishedEvent().roomListChanged()).isFalse();
            assertThat(publishedEvent().latestMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("삭제는")
    class Describe_delete {

        @Test
        @DisplayName("it_최신 메시지를 지우면 반영 후 다시 찾은 최신 메시지로 알린다")
        void it_deletes_latest_and_publishes_replacement() {
            ChatMessage target = message(7L, MessageType.IMAGE, null);
            ChatMessage previous = message(6L, MessageType.TEXT, null);
            when(messagePort.findForUpdate(7L)).thenReturn(Optional.of(target));
            when(messagePort.findLatest(ROOM)).thenReturn(Optional.of(target), Optional.of(previous));

            service.execute(ACTOR, 7L);

            var order = inOrder(messagePort, mutationPort);
            order.verify(messagePort).findLatest(ROOM);
            order.verify(messagePort).saveDeletion(argThat(ChatMessage::isDeleted));
            order.verify(messagePort).findLatest(ROOM);
            order.verify(mutationPort).publish(any());
            ChatMessageMutationEvent event = publishedEvent();
            assertThat(event.kind()).isEqualTo(ChatMessageMutationEvent.Kind.DELETED);
            assertThat(event.latestMessage().messageId()).isEqualTo(6L);
        }

        @Test
        @DisplayName("it_유일한 메시지를 지우면 latestMessage 가 null 이다")
        void it_deletes_only_message() {
            ChatMessage target = message(7L, MessageType.TEXT, null);
            when(messagePort.findForUpdate(7L)).thenReturn(Optional.of(target));
            when(messagePort.findLatest(ROOM)).thenReturn(Optional.of(target), Optional.empty());

            service.execute(ACTOR, 7L);

            assertThat(publishedEvent().roomListChanged()).isTrue();
            assertThat(publishedEvent().latestMessage()).isNull();
        }

        @Test
        @DisplayName("it_타인 메시지는 403 이고 쓰지 않는다")
        void it_rejects_other_author() {
            when(messagePort.findForUpdate(7L)).thenReturn(Optional.of(message(7L, MessageType.TEXT, null)));

            assertThatThrownBy(() -> service.execute(new ActorContext(2L, ActorContext.Role.ROLE_USER), 7L))
                    .isInstanceOf(GlobalException.class).extracting("errorCode").isEqualTo(ErrorCode.CHAT_MESSAGE_FORBIDDEN);
            verify(messagePort, never()).saveDeletion(any());
        }
    }
}
