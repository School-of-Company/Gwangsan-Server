package team.startup.gwangsan.domain.chat.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChatMessage 단위 테스트")
class ChatMessageTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 1, 12, 0);

    private static ChatMessage message(Long senderId, MessageType type) {
        return new ChatMessage(7L, 3L, senderId, "원문", null, type, CREATED, null, null);
    }

    @Nested
    @DisplayName("assertMutableBy() 는")
    class Describe_assertMutableBy {

        @Test
        @DisplayName("it_타인 또는 발신자 없는 메시지면 403 을 먼저 던진다")
        void it_rejects_other_or_missing_sender() {
            LocalDateTime expired = CREATED.plusDays(2);
            assertThatThrownBy(() -> message(1L, MessageType.TEXT).assertMutableBy(2L, expired))
                    .isInstanceOf(GlobalException.class).extracting("errorCode").isEqualTo(ErrorCode.CHAT_MESSAGE_FORBIDDEN);
            assertThatThrownBy(() -> message(null, MessageType.TEXT).assertMutableBy(1L, CREATED))
                    .isInstanceOf(GlobalException.class).extracting("errorCode").isEqualTo(ErrorCode.CHAT_MESSAGE_FORBIDDEN);
        }

        @Test
        @DisplayName("it_정확히 24시간까지 허용하고 넘으면 409 를 던진다")
        void it_allows_until_24_hours() {
            message(1L, MessageType.TEXT).assertMutableBy(1L, CREATED.plusHours(24));
            assertThatThrownBy(() -> message(1L, MessageType.TEXT).assertMutableBy(1L, CREATED.plusHours(24).plusNanos(1)))
                    .isInstanceOf(GlobalException.class).extracting("errorCode").isEqualTo(ErrorCode.CHAT_MESSAGE_EXPIRED);
        }
    }

    @Nested
    @DisplayName("edit() 는")
    class Describe_edit {

        @Test
        @DisplayName("it_텍스트가 아니면 400, 빈 내용이면 400 을 던진다")
        void it_rejects_non_text_and_blank() {
            assertThatThrownBy(() -> message(1L, MessageType.IMAGE).edit("수정", CREATED))
                    .isInstanceOf(GlobalException.class).extracting("errorCode").isEqualTo(ErrorCode.CHAT_MESSAGE_NOT_TEXT);
            assertThatThrownBy(() -> message(1L, MessageType.TEXT).edit(" ", CREATED))
                    .isInstanceOf(GlobalException.class).extracting("errorCode").isEqualTo(ErrorCode.CHAT_MESSAGE_INVALID_CONTENT);
            assertThatThrownBy(() -> message(1L, MessageType.TEXT).edit(null, CREATED))
                    .isInstanceOf(GlobalException.class).extracting("errorCode").isEqualTo(ErrorCode.CHAT_MESSAGE_INVALID_CONTENT);
        }

        @Test
        @DisplayName("it_첫 수정 전 원문을 계속 보존한다 (replay 비교 기준)")
        void it_keeps_first_original_content() {
            ChatMessage once = message(1L, MessageType.TEXT).edit("첫 수정", CREATED.plusMinutes(1));
            ChatMessage twice = once.edit("두 번째", CREATED.plusMinutes(2));

            assertThat(once.originalContent()).isEqualTo("원문");
            assertThat(twice.content()).isEqualTo("두 번째");
            assertThat(twice.originalContent()).isEqualTo("원문");
            assertThat(twice.editedAt()).isEqualTo(CREATED.plusMinutes(2));
        }

        @Test
        @DisplayName("it_delete() 는 deletedAt 만 기록한다")
        void it_deletes() {
            ChatMessage deleted = message(1L, MessageType.TEXT).delete(CREATED.plusHours(1));
            assertThat(deleted.isDeleted()).isTrue();
            assertThat(deleted.content()).isEqualTo("원문");
        }
    }
}
